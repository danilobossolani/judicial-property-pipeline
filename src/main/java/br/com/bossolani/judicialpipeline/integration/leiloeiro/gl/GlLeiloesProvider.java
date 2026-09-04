package br.com.bossolani.judicialpipeline.integration.leiloeiro.gl;

import br.com.bossolani.judicialpipeline.exception.LoteDescartadoException;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.ColetaLeiloeiroDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.LeiloeiroProvider;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.ResilienciaFonteService;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.DadosDinamicosLeilaoDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteDescobertoDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteLeilaoDTO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(50)
public class GlLeiloesProvider implements LeiloeiroProvider {

    private static final String NOME = "GL Leilões";
    private static final String DOMINIO = "glleiloes.com.br";
    private static final String URL_BASE = "https://www.glleiloes.com.br";
    private static final String URL_LISTAGEM = URL_BASE + "/lotes/imovel";

    private static final Pattern CAMINHO_LOTE = Pattern.compile(
            "^/item/\\d+/detalhes/?$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PROCESSO = Pattern.compile(
            "(?i)processo\\s*:\\s*(\\d{7}[-‑]\\d{2}\\.\\d{4}\\.\\d\\.\\d{2}\\.\\d{4}|\\d{20})"
    );

    private static final Pattern CIDADE = Pattern.compile(
            "(?i)cidade\\s*:\\s*(Sorocaba|Votorantim)\\s*/\\s*SP"
    );

    private static final Pattern DATA_ABERTURA = Pattern.compile(
            "(?i)data\\s+de\\s+abertura\\s+para\\s+lances\\s*:\\s*"
                    + "(\\d{2}/\\d{2}/\\d{4})\\s*(?:às|as)?\\s*(\\d{1,2}:\\d{2})"
    );

    private static final Pattern DATA_PRACA = Pattern.compile(
            "(?i)data\\s+([12])[ºo°]?\\s+leil[aã]o\\s*:\\s*"
                    + "(\\d{2}/\\d{2}/\\d{4})\\s*(?:às|as)?\\s*(\\d{1,2}:\\d{2})"
                    + "\\s+lance\\s+inicial\\s*:\\s*(?:R\\$\\s*([\\d.]+,\\d{2}))?"
    );

    private static final Pattern VALOR_AVALIACAO = Pattern.compile(
            "(?i)valor\\s+de\\s+avalia[cç][aã]o\\s*:\\s*R\\$\\s*([\\d.]+,\\d{2})"
    );

    private static final Pattern LANCE_DESTAQUE = Pattern.compile(
            "(?i)lance\\s+inicial\\s+R\\$\\s*([\\d.]+,\\d{2})"
    );

    private static final Pattern INCREMENTO = Pattern.compile(
            "(?i)incremento\\s+m[ií]nimo\\s*:\\s*R\\$\\s*([\\d.]+,\\d{2})"
    );

    private static final Pattern COMISSAO = Pattern.compile(
            "(?i)comiss[aã]o\\s*\\((\\d+(?:[.,]\\d+)?)%\\)"
    );

    private static final Pattern VARA = Pattern.compile(
            "(?i)vara\\s*:\\s*(.+?)(?=\\s+comarca\\s*:|\\s+exequente\\s*:|$)"
    );

    private static final Pattern LOCALIZACAO = Pattern.compile(
            "(?i)localiza[cç][aã]o\\s+do\\s+im[oó]vel\\s+endere[cç]o\\s*:\\s*"
                    + "(.+?)\\s+cidade\\s*:\\s*(Sorocaba|Votorantim)\\s*/\\s*SP"
    );

    private static final Pattern ENDERECO_SIMPLES = Pattern.compile(
            "(?i)endere[cç]o\\s*:\\s*(.+?)(?=\\s+descri[cç][aã]o\\s*:|$)"
    );

    private static final Pattern PAGINA = Pattern.compile(
            "(?:[?&])page=(\\d+)",
            Pattern.CASE_INSENSITIVE
    );

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern(
            "dd/MM/yyyy H:mm",
            Locale.forLanguageTag("pt-BR")
    );

    private final ResilienciaFonteService resiliencia;
    private final int timeoutMs;
    private final Clock clock;
    private final CarregadorPagina carregadorPagina;

    @Autowired
    public GlLeiloesProvider(
            ResilienciaFonteService resiliencia,
            @Value("${integracao.fontes.timeout-ms:15000}") int timeoutMs
    ) {
        this(resiliencia, timeoutMs, Clock.systemDefaultZone(), null);
    }

    GlLeiloesProvider(
            ResilienciaFonteService resiliencia,
            int timeoutMs,
            Clock clock,
            CarregadorPagina carregadorPagina
    ) {
        this.resiliencia = resiliencia;
        this.timeoutMs = Math.max(1000, timeoutMs);
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
        this.carregadorPagina = carregadorPagina == null
                ? this::buscarPaginaHttp
                : carregadorPagina;
    }

    @Override
    public String nome() {
        return NOME;
    }

    @Override
    public boolean suporta(URI uri) {
        if (uri == null || uri.getHost() == null
                || !"https".equalsIgnoreCase(uri.getScheme())) {
            return false;
        }

        String host = uri.getHost().toLowerCase(Locale.ROOT);
        return (host.equals(DOMINIO) || host.endsWith("." + DOMINIO))
                && CAMINHO_LOTE.matcher(uri.getPath()).matches();
    }

    @Override
    public String normalizarUrl(URI uri) {
        if (!suporta(uri)) {
            throw new IllegalArgumentException("URL de lote da GL Leilões inválida");
        }

        try {
            return new URI(
                    "https",
                    null,
                    "www." + DOMINIO,
                    -1,
                    removerBarraFinal(uri.getPath()),
                    null,
                    null
            ).toASCIIString();
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException(
                    "URL de lote da GL Leilões inválida",
                    exception
            );
        }
    }

    @Override
    public List<LoteDescobertoDTO> descobrirLotes() throws Exception {
        return resiliencia.executar(NOME, this::descobrirSemRetentativa);
    }

    @Override
    public ColetaLeiloeiroDTO coletar(String url) throws Exception {
        return resiliencia.executar(NOME, () -> coletarSemRetentativa(url));
    }

    private List<LoteDescobertoDTO> descobrirSemRetentativa() throws Exception {
        Map<String, LoteDescobertoDTO> lotes = new LinkedHashMap<>();
        int paginaAtual = 1;
        int totalPaginas = 1;

        do {
            String url = URL_LISTAGEM + (paginaAtual == 1 ? "" : "?page=" + paginaAtual);
            Document documento = carregadorPagina.carregar(url);
            totalPaginas = Math.max(totalPaginas, totalPaginas(documento));

            for (Element cartao : documento.select(".lista-lotes .lote, .lote.card, .lote")) {
                Element link = cartao.selectFirst("a[href*=/item/][href*=/detalhes]");
                if (link == null) {
                    continue;
                }

                URI uri;
                try {
                    uri = URI.create(link.absUrl("href"));
                } catch (IllegalArgumentException exception) {
                    continue;
                }

                if (!suporta(uri)) {
                    continue;
                }

                String texto = texto(cartao);
                String cidade = extrairGrupo(texto, CIDADE, 1);
                if (cidade == null) {
                    continue;
                }

                String urlNormalizada = normalizarUrl(uri);
                lotes.putIfAbsent(
                        urlNormalizada,
                        new LoteDescobertoDTO(
                                urlNormalizada,
                                tituloCartao(cartao, cidade),
                                capitalizarCidade(cidade),
                                limitarTexto(texto, 4000),
                                NOME
                        )
                );
            }

            paginaAtual++;
        } while (paginaAtual <= totalPaginas);

        return new ArrayList<>(lotes.values());
    }

    private ColetaLeiloeiroDTO coletarSemRetentativa(String url) throws Exception {
        String urlNormalizada = normalizarUrl(URI.create(url));
        Document documento = carregadorPagina.carregar(urlNormalizada);
        Element bloco = documento.selectFirst(".detalhes-lote");
        String textoPagina = texto(bloco == null ? documento.body() : bloco);
        String cidade = extrairGrupo(textoPagina, CIDADE, 1);

        if (cidade == null) {
            throw new LoteDescartadoException(
                    "Lote da GL Leilões fora de Sorocaba e Votorantim."
            );
        }

        String processo = extrairGrupo(textoPagina, PROCESSO, 1);
        if (processo == null) {
            throw new LoteDescartadoException(
                    "Lote judicial da GL Leilões sem número de processo CNJ."
            );
        }

        String titulo = tituloDocumento(documento);
        EnderecoGl endereco = extrairEndereco(textoPagina, capitalizarCidade(cidade));
        Map<Integer, PracaGl> pracas = extrairPracas(textoPagina);
        PracaGl primeira = pracas.get(1);
        PracaGl segunda = pracas.get(2);
        LocalDateTime abertura = extrairData(textoPagina, DATA_ABERTURA, 1, 2);
        BigDecimal avaliacao = extrairValor(textoPagina, VALOR_AVALIACAO);
        BigDecimal destaque = extrairValor(textoPagina, LANCE_DESTAQUE);
        BigDecimal lanceMinimo = menorValor(
                valor(primeira),
                valor(segunda),
                destaque
        );
        Integer desconto = calcularDesconto(avaliacao, lanceMinimo);

        LoteLeilaoDTO lote = new LoteLeilaoDTO(
                normalizarProcesso(processo),
                avaliacao,
                capitalizarCidade(cidade),
                valorOuNulo(extrairGrupo(textoPagina, VARA, 1)),
                inferirTipo(titulo, textoPagina),
                endereco.logradouro(),
                endereco.numero(),
                endereco.bairro(),
                urlNormalizada
        );

        DadosDinamicosLeilaoDTO leilao = new DadosDinamicosLeilaoDTO(
                abertura,
                data(primeira),
                valor(primeira) == null ? avaliacao : valor(primeira),
                data(primeira),
                data(segunda),
                valor(segunda),
                desconto,
                definirStatus(textoPagina, primeira, segunda),
                definirResultado(textoPagina),
                lanceMinimo == null ? avaliacao : lanceMinimo,
                extrairValor(textoPagina, INCREMENTO),
                extrairDecimal(textoPagina, COMISSAO)
        );

        return new ColetaLeiloeiroDTO(lote, leilao);
    }

    private Map<Integer, PracaGl> extrairPracas(String texto) {
        Map<Integer, PracaGl> pracas = new LinkedHashMap<>();
        Matcher matcher = DATA_PRACA.matcher(texto);

        while (matcher.find()) {
            int ordem = Integer.parseInt(matcher.group(1));
            pracas.putIfAbsent(
                    ordem,
                    new PracaGl(
                            parseData(matcher.group(2), matcher.group(3)),
                            monetario(matcher.group(4))
                    )
            );
        }
        return pracas;
    }

    private EnderecoGl extrairEndereco(String texto, String cidade) {
        Matcher localizacao = LOCALIZACAO.matcher(texto);
        String completo = null;

        while (localizacao.find()) {
            completo = localizacao.group(1).trim();
        }

        if (completo == null) {
            completo = extrairGrupo(texto, ENDERECO_SIMPLES, 1);
        }

        if (completo == null || completo.isBlank()) {
            return new EnderecoGl("Endereço informado no edital", null, null);
        }

        String semCidade = completo
                .replaceAll("(?i)\\s*,?\\s*" + Pattern.quote(cidade) + "\\s*[-/]?\\s*SP\\s*$", "")
                .trim();
        String[] bairroSeparado = semCidade.split("\\s+-\\s+", 2);
        String trecho = bairroSeparado[0].trim();
        String bairro = bairroSeparado.length > 1 ? bairroSeparado[1].trim() : null;
        Matcher numeroMatcher = Pattern.compile(",\\s*(?:n[º°o.]?\\s*)?([^,]+)$", Pattern.CASE_INSENSITIVE)
                .matcher(trecho);
        String numero = null;
        String logradouro = trecho;

        if (numeroMatcher.find()) {
            numero = numeroMatcher.group(1).trim();
            logradouro = trecho.substring(0, numeroMatcher.start()).trim();
        }

        return new EnderecoGl(
                valorOuNulo(logradouro),
                valorOuNulo(numero),
                valorOuNulo(bairro)
        );
    }

    private String definirStatus(String texto, PracaGl primeira, PracaGl segunda) {
        String normalizado = normalizar(texto);
        if (normalizado.contains("cancelad")) {
            return "CANCELADO";
        }
        if (normalizado.contains("suspens")) {
            return "SUSPENSO";
        }
        if (normalizado.contains("sem licitante")
                || normalizado.contains("encerrado")
                || normalizado.contains("arrematado")) {
            return "ENCERRADO";
        }

        LocalDateTime agora = LocalDateTime.now(clock);
        LocalDateTime encerramento = segunda != null ? segunda.data() : data(primeira);
        if (encerramento != null && agora.isAfter(encerramento)) {
            return "ENCERRADO";
        }
        if (primeira != null && agora.isBefore(primeira.data())) {
            return "AGENDADO";
        }
        return "EM_ANDAMENTO";
    }

    private String definirResultado(String texto) {
        String normalizado = normalizar(texto);
        if (normalizado.contains("sem licitante") || normalizado.contains("sem lances")) {
            return "SEM LANCES";
        }
        if (normalizado.contains("arrematado") || normalizado.contains("vendido")) {
            return "ARREMATADO";
        }
        if (normalizado.contains("com lances")) {
            return "COM LANCES";
        }
        return "DESCONHECIDO";
    }

    private String tituloCartao(Element cartao, String cidade) {
        Element titulo = cartao.selectFirst("h1, h2, h3, h4, h5, .card-title");
        String valor = titulo == null ? "" : titulo.text().trim();
        return valor.isBlank() ? "Imóvel em " + capitalizarCidade(cidade) : valor;
    }

    private String tituloDocumento(Document documento) {
        String titulo = documento.title();
        return titulo.replaceFirst("(?i)\\s+-\\s+Lote.*$", "").trim();
    }

    private String inferirTipo(String titulo, String texto) {
        String normalizado = normalizar(titulo + " " + texto);
        if (normalizado.contains("apartamento")) return "Apartamento";
        if (normalizado.contains("galpao") || normalizado.contains("industrial")) return "Galpão";
        if (normalizado.contains("terreno") || normalizado.contains("gleba")) return "Terreno";
        if (normalizado.contains("sobrado")) return "Sobrado";
        if (normalizado.contains("casa")) return "Casa";
        if (normalizado.contains("predio")) return "Prédio";
        if (normalizado.contains("sala")) return "Sala";
        return "Imóvel";
    }

    private int totalPaginas(Document documento) {
        int total = 1;
        for (Element link : documento.select("a[href*=?page=]")) {
            Matcher matcher = PAGINA.matcher(link.attr("href"));
            if (matcher.find()) {
                total = Math.max(total, Integer.parseInt(matcher.group(1)));
            }
        }
        return total;
    }

    private LocalDateTime extrairData(
            String texto,
            Pattern pattern,
            int grupoData,
            int grupoHora
    ) {
        Matcher matcher = pattern.matcher(texto);
        return matcher.find() ? parseData(matcher.group(grupoData), matcher.group(grupoHora)) : null;
    }

    private LocalDateTime parseData(String data, String hora) {
        try {
            return LocalDateTime.parse(data + " " + hora, DATA_HORA);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Data de leilão inválida na GL Leilões", exception);
        }
    }

    private BigDecimal extrairValor(String texto, Pattern pattern) {
        Matcher matcher = pattern.matcher(texto == null ? "" : texto);
        return matcher.find() ? monetario(matcher.group(1)) : null;
    }

    private BigDecimal extrairDecimal(String texto, Pattern pattern) {
        Matcher matcher = pattern.matcher(texto == null ? "" : texto);
        return matcher.find() ? new BigDecimal(matcher.group(1).replace(',', '.')) : null;
    }

    private BigDecimal monetario(String valor) {
        return valor == null || valor.isBlank()
                ? null
                : new BigDecimal(valor.replace(".", "").replace(',', '.'));
    }

    private BigDecimal menorValor(BigDecimal... valores) {
        BigDecimal menor = null;
        for (BigDecimal valor : valores) {
            if (valor != null && (menor == null || valor.compareTo(menor) < 0)) {
                menor = valor;
            }
        }
        return menor == null ? null : menor.setScale(2, RoundingMode.HALF_UP);
    }

    private Integer calcularDesconto(BigDecimal avaliacao, BigDecimal lance) {
        if (avaliacao == null || lance == null || avaliacao.signum() <= 0) {
            return null;
        }
        int percentualLance = lance.multiply(BigDecimal.valueOf(100))
                .divide(avaliacao, 0, RoundingMode.HALF_UP)
                .intValue();
        return Math.max(0, 100 - percentualLance);
    }

    private String extrairGrupo(String texto, Pattern pattern, int grupo) {
        Matcher matcher = pattern.matcher(texto == null ? "" : texto);
        return matcher.find() ? matcher.group(grupo).trim() : null;
    }

    private String normalizarProcesso(String processo) {
        return processo == null ? null : processo.replace('‑', '-');
    }

    private String capitalizarCidade(String cidade) {
        return "votorantim".equals(normalizar(cidade)) ? "Votorantim" : "Sorocaba";
    }

    private String texto(Element elemento) {
        return elemento == null ? "" : elemento.text().replaceAll("\\s+", " ").trim();
    }

    private String normalizar(String texto) {
        return Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private String limitarTexto(String texto, int limite) {
        return texto.length() <= limite ? texto : texto.substring(0, limite);
    }

    private String valorOuNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private String removerBarraFinal(String caminho) {
        return caminho != null && caminho.length() > 1 && caminho.endsWith("/")
                ? caminho.substring(0, caminho.length() - 1)
                : caminho;
    }

    private LocalDateTime data(PracaGl praca) {
        return praca == null ? null : praca.data();
    }

    private BigDecimal valor(PracaGl praca) {
        return praca == null ? null : praca.valor();
    }

    private Document buscarPaginaHttp(String url) throws Exception {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/128 Safari/537.36")
                .header("Accept-Language", "pt-BR,pt;q=0.9")
                .referrer(URL_BASE + "/")
                .timeout(timeoutMs)
                .maxBodySize(0)
                .get();
    }

    @FunctionalInterface
    interface CarregadorPagina {
        Document carregar(String url) throws Exception;
    }

    private record PracaGl(LocalDateTime data, BigDecimal valor) {
    }

    private record EnderecoGl(String logradouro, String numero, String bairro) {
    }
}
