package br.com.bossolani.judicialpipeline.integration.leiloeiro.zuk;

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
@Order(40)
public class PortalZukProvider
        implements LeiloeiroProvider {

    private static final String NOME =
            "Portal Zuk";

    private static final String DOMINIO =
            "portalzuk.com.br";

    private static final String URL_BASE =
            "https://www.portalzuk.com.br";

    private static final String CAMINHO_LISTAGEM =
            "/leilao-de-imoveis/v/leilao-judicial-sao-paulo-tjsp/"
                    + "c/todos-imoveis/sp/interior/";

    private static final Pattern CAMINHO_IMOVEL =
            Pattern.compile(
                    "^/imovel/sp/(sorocaba|votorantim)/.+/\\d+-\\d+/?$",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern NUMERO_PROCESSO =
            Pattern.compile(
                    "\\d{7}-\\d{2}\\.\\d{4}\\.\\d\\.\\d{2}\\.\\d{4}"
            );

    private static final Pattern VALOR_MONETARIO =
            Pattern.compile(
                    "R\\$\\s*([\\d.]+,\\d{2})"
            );

    private static final Pattern DATA_HORA =
            Pattern.compile(
                    "(\\d{2}/\\d{2}/\\d{2,4})[^0-9]{0,12}"
                            + "(\\d{1,2})h(\\d{2})",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern ORDEM_ETAPA =
            Pattern.compile(
                    "^\\s*([12])\\D{0,4}leil",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern PERCENTUAL_ANTES_VALOR =
            Pattern.compile(
                    "(?:^|\\s)(\\d{1,3})\\s*%?\\s+R\\$",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern COMISSAO =
            Pattern.compile(
                    "comiss[aã]o.{0,80}?(\\d+(?:[.,]\\d+)?)\\s*%",
                    Pattern.CASE_INSENSITIVE
            );

    private static final DateTimeFormatter DATA_DOIS_DIGITOS =
            DateTimeFormatter.ofPattern(
                    "dd/MM/yy HH:mm",
                    Locale.forLanguageTag("pt-BR")
            );

    private static final DateTimeFormatter DATA_QUATRO_DIGITOS =
            DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy HH:mm",
                    Locale.forLanguageTag("pt-BR")
            );

    private static final List<String> CIDADES =
            List.of(
                    "Sorocaba",
                    "Votorantim"
            );

    private final ResilienciaFonteService resiliencia;

    private final int timeoutMs;

    private final Clock clock;

    private final CarregadorPagina carregadorPagina;


    @Autowired
    public PortalZukProvider(
            ResilienciaFonteService resiliencia,
            @Value("${integracao.fontes.timeout-ms:15000}")
            int timeoutMs
    ) {

        this(
                resiliencia,
                timeoutMs,
                Clock.systemDefaultZone(),
                null
        );
    }


    PortalZukProvider(
            ResilienciaFonteService resiliencia,
            int timeoutMs,
            Clock clock,
            CarregadorPagina carregadorPagina
    ) {

        this.resiliencia = resiliencia;
        this.timeoutMs = Math.max(1000, timeoutMs);
        this.clock = clock;
        this.carregadorPagina =
                carregadorPagina != null
                        ? carregadorPagina
                        : this::buscarPaginaHttp;
    }


    @Override
    public String nome() {

        return NOME;
    }


    @Override
    public boolean suporta(
            URI uri
    ) {

        if (uri == null
                || uri.getHost() == null
                || !"https".equalsIgnoreCase(uri.getScheme())) {
            return false;
        }

        String host = uri.getHost().toLowerCase(Locale.ROOT);

        return (host.equals(DOMINIO)
                || host.endsWith("." + DOMINIO))
                && CAMINHO_IMOVEL.matcher(uri.getPath()).matches();
    }


    @Override
    public String normalizarUrl(
            URI uri
    ) {

        if (!suporta(uri)) {
            throw new IllegalArgumentException(
                    "URL de imóvel do Portal Zuk inválida"
            );
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
                    "URL de imóvel do Portal Zuk inválida",
                    exception
            );
        }
    }


    @Override
    public List<LoteDescobertoDTO> descobrirLotes()
            throws Exception {

        return resiliencia.executar(
                NOME,
                this::descobrirSemRetentativa
        );
    }


    @Override
    public ColetaLeiloeiroDTO coletar(
            String url
    ) throws Exception {

        return resiliencia.executar(
                NOME,
                () -> coletarSemRetentativa(url)
        );
    }


    private List<LoteDescobertoDTO> descobrirSemRetentativa()
            throws Exception {

        Map<String, LoteDescobertoDTO> lotes =
                new LinkedHashMap<>();

        for (String cidade : CIDADES) {
            Document documento = carregadorPagina.carregar(
                    CAMINHO_LISTAGEM + normalizar(cidade)
            );

            for (Element cartao : documento.select(".card-property")) {
                Element link = cartao.selectFirst(
                        "a[href*=/imovel/][href]"
                );

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

                String urlNormalizada = normalizarUrl(uri);
                String titulo = link.attr("title").trim();

                if (titulo.isBlank()) {
                    titulo = tituloDoCartao(cartao, cidade);
                }

                lotes.putIfAbsent(
                        urlNormalizada,
                        new LoteDescobertoDTO(
                                urlNormalizada,
                                titulo,
                                cidade,
                                limitarTexto(cartao.text(), 4000),
                                NOME
                        )
                );
            }
        }

        return new ArrayList<>(lotes.values());
    }


    private ColetaLeiloeiroDTO coletarSemRetentativa(
            String url
    ) throws Exception {

        String urlNormalizada = normalizarUrl(URI.create(url));
        Document documento = carregadorPagina.carregar(urlNormalizada);
        String titulo = tituloDocumento(documento);
        String enderecoCompleto = texto(
                documento.selectFirst(".property-address")
        );
        EnderecoZuk endereco = separarEndereco(enderecoCompleto);
        String textoCompleto = documento.text();
        String processo = extrairProcesso(textoCompleto);
        List<EtapaLeilao> etapas = extrairEtapas(documento);
        EtapaLeilao primeira = etapa(etapas, 1);
        EtapaLeilao segunda = etapa(etapas, 2);
        BigDecimal avaliacao = primeira != null
                ? primeira.valor()
                : extrairAvaliacao(textoCompleto);
        Integer percentual = segunda == null
                ? null
                : segunda.percentual();
        BigDecimal lanceMinimo = definirLanceMinimo(
                primeira,
                segunda
        );
        Integer desconto;

        if (percentual == null) {
            desconto = calcularDesconto(avaliacao, lanceMinimo);
        } else {
            desconto = Math.max(0, 100 - percentual);
        }
        String textoSituacao = texto(
                documento.selectFirst(".card-action")
        );
        String status = definirStatus(
                textoSituacao,
                primeira,
                segunda
        );

        LoteLeilaoDTO lote = new LoteLeilaoDTO(
                processo,
                avaliacao,
                endereco.cidade(),
                null,
                inferirTipo(titulo, textoCompleto),
                endereco.logradouro(),
                endereco.numero(),
                endereco.bairro(),
                urlNormalizada
        );

        DadosDinamicosLeilaoDTO leilao =
                new DadosDinamicosLeilaoDTO(
                        primeira == null ? null : primeira.data(),
                        primeira == null ? null : primeira.data(),
                        primeira == null ? null : primeira.valor(),
                        segunda == null ? null : segunda.data(),
                        segunda == null ? null : segunda.data(),
                        segunda == null ? null : segunda.valor(),
                        desconto,
                        status,
                        definirResultado(textoSituacao, textoCompleto),
                        lanceMinimo,
                        null,
                        extrairComissao(textoCompleto)
                );

        return new ColetaLeiloeiroDTO(lote, leilao);
    }


    private List<EtapaLeilao> extrairEtapas(
            Document documento
    ) {

        List<EtapaLeilao> etapas = new ArrayList<>();

        for (Element item : documento.select(".card-action-item")) {
            String conteudo = item.text().replaceAll("\\s+", " ").trim();
            Matcher ordemMatcher = ORDEM_ETAPA.matcher(
                    normalizar(conteudo)
            );
            int ordem = ordemMatcher.find()
                    ? Integer.parseInt(ordemMatcher.group(1))
                    : 0;

            if (ordem == 0) {
                continue;
            }

            LocalDateTime data = extrairData(conteudo);
            BigDecimal valor = extrairValor(conteudo);
            Integer percentual = ordem == 2
                    ? extrairPercentual(conteudo)
                    : null;

            if (data != null || valor != null) {
                etapas.add(
                        new EtapaLeilao(
                                ordem,
                                data,
                                valor,
                                percentual
                        )
                );
            }
        }

        return etapas;
    }


    private EtapaLeilao etapa(
            List<EtapaLeilao> etapas,
            int ordem
    ) {

        return etapas.stream()
                .filter(item -> item.ordem() == ordem)
                .findFirst()
                .orElse(null);
    }


    private LocalDateTime extrairData(
            String conteudo
    ) {

        Matcher matcher = DATA_HORA.matcher(conteudo);

        if (!matcher.find()) {
            return null;
        }

        String valor = matcher.group(1)
                + " "
                + matcher.group(2)
                + ":"
                + matcher.group(3);

        try {
            return LocalDateTime.parse(
                    valor,
                    matcher.group(1).length() == 8
                            ? DATA_DOIS_DIGITOS
                            : DATA_QUATRO_DIGITOS
            );

        } catch (DateTimeParseException exception) {
            return null;
        }
    }


    private BigDecimal extrairValor(
            String conteudo
    ) {

        Matcher matcher = VALOR_MONETARIO.matcher(
                conteudo == null ? "" : conteudo
        );

        return matcher.find()
                ? monetario(matcher.group(1))
                : null;
    }


    private Integer extrairPercentual(
            String conteudo
    ) {

        Matcher matcher = PERCENTUAL_ANTES_VALOR.matcher(conteudo);

        if (!matcher.find()) {
            return null;
        }

        int percentual = Integer.parseInt(matcher.group(1));

        return percentual > 0 && percentual <= 100
                ? percentual
                : null;
    }


    private BigDecimal extrairAvaliacao(
            String texto
    ) {

        Matcher matcher = Pattern.compile(
                "avalia[cç][aã]o.{0,80}?R\\$\\s*([\\d.]+,\\d{2})",
                Pattern.CASE_INSENSITIVE
        ).matcher(texto);

        return matcher.find()
                ? monetario(matcher.group(1))
                : null;
    }


    private BigDecimal definirLanceMinimo(
            EtapaLeilao primeira,
            EtapaLeilao segunda
    ) {

        LocalDateTime agora = LocalDateTime.now(clock);

        if (segunda != null
                && segunda.data() != null
                && !agora.isBefore(segunda.data())) {
            return segunda.valor();
        }

        if (primeira != null
                && primeira.data() != null
                && !agora.isAfter(primeira.data())) {
            return primeira.valor();
        }

        if (segunda != null && segunda.valor() != null) {
            return segunda.valor();
        }

        return primeira == null ? null : primeira.valor();
    }


    private String definirStatus(
            String situacao,
            EtapaLeilao primeira,
            EtapaLeilao segunda
    ) {

        String normalizado = normalizar(situacao);

        if (normalizado.contains("cancelad")) {
            return "CANCELADO";
        }

        if (normalizado.contains("suspens")) {
            return "SUSPENSO";
        }

        if (normalizado.contains("vendid")
                || normalizado.contains("arrematad")) {
            return "ENCERRADO";
        }

        LocalDateTime agora = LocalDateTime.now(clock);
        LocalDateTime finalLeilao = segunda != null
                && segunda.data() != null
                ? segunda.data()
                : primeira == null
                ? null
                : primeira.data();

        if (finalLeilao != null && agora.isAfter(finalLeilao)) {
            return "ENCERRADO";
        }

        if (primeira != null
                && primeira.data() != null
                && agora.isBefore(primeira.data())) {
            return "AGENDADO";
        }

        return "EM_ANDAMENTO";
    }


    private String definirResultado(
            String situacao,
            String textoCompleto
    ) {

        String normalizado = normalizar(
                situacao + " " + textoCompleto
        );

        if (normalizado.contains("sem licitante")
                || normalizado.contains("sem lances")) {
            return "SEM LANCES";
        }

        if (normalizado.contains("vendido")
                || normalizado.contains("arrematado")) {
            return "ARREMATADO";
        }

        return "DESCONHECIDO";
    }


    private String extrairProcesso(
            String texto
    ) {

        Matcher matcher = NUMERO_PROCESSO.matcher(texto);

        if (!matcher.find()) {
            throw new IllegalArgumentException(
                    "Imóvel do Portal Zuk sem número de processo CNJ"
            );
        }

        return matcher.group();
    }


    private EnderecoZuk separarEndereco(
            String enderecoCompleto
    ) {

        if (enderecoCompleto == null || enderecoCompleto.isBlank()) {
            throw new IllegalArgumentException(
                    "Imóvel do Portal Zuk sem endereço"
            );
        }

        Matcher cidadeMatcher = Pattern.compile(
                "(?i)\\b(Sorocaba|Votorantim)\\s*/\\s*SP\\s*$"
        ).matcher(enderecoCompleto);

        if (!cidadeMatcher.find()) {
            throw new IllegalArgumentException(
                    "Imóvel do Portal Zuk fora de Sorocaba e Votorantim"
            );
        }

        String cidade = capitalizar(cidadeMatcher.group(1));
        String semCidade = enderecoCompleto
                .substring(0, cidadeMatcher.start())
                .replaceFirst("\\s*-\\s*$", "")
                .trim();
        String[] partes = semCidade.split("\\s+-\\s+");
        String trechoEndereco = partes[0].trim();
        String bairro = partes.length > 1
                ? partes[partes.length - 1].trim()
                : null;
        Matcher numeroMatcher = Pattern.compile(
                ",\\s*([^,]+)$"
        ).matcher(trechoEndereco);
        String numero = numeroMatcher.find()
                ? numeroMatcher.group(1).trim()
                : null;
        String logradouro = numeroMatcher.find(0)
                ? trechoEndereco.substring(0, numeroMatcher.start()).trim()
                : trechoEndereco;

        return new EnderecoZuk(
                logradouro,
                numero,
                valorOuNulo(bairro),
                cidade
        );
    }


    private String tituloDocumento(
            Document documento
    ) {

        Element meta = documento.selectFirst("meta[property=og:title]");
        String titulo = meta == null
                ? documento.title()
                : meta.attr("content");

        return titulo
                .replaceFirst("(?i)\\s*[|\u2013-]\\s*Zuk.*$", "")
                .trim();
    }


    private String tituloDoCartao(
            Element cartao,
            String cidade
    ) {

        String conteudo = cartao.text();
        String tipo = inferirTipo(conteudo, conteudo);

        return tipo + " em " + cidade;
    }


    private String inferirTipo(
            String titulo,
            String textoCompleto
    ) {

        String normalizado = normalizar(titulo + " " + textoCompleto);

        if (normalizado.contains("apartamento")) {
            return "Apartamento";
        }

        if (normalizado.contains("galpao")
                || normalizado.contains("industrial")) {
            return "Galpão";
        }

        if (normalizado.contains("terreno")
                || normalizado.contains("gleba")) {
            return "Terreno";
        }

        if (normalizado.contains("casa")
                || normalizado.contains("sobrado")) {
            return "Casa";
        }

        if (normalizado.contains("predio")) {
            return "Prédio";
        }

        if (normalizado.contains("sala")) {
            return "Sala";
        }

        return "Imóvel";
    }


    private BigDecimal extrairComissao(
            String texto
    ) {

        Matcher matcher = COMISSAO.matcher(texto);

        return matcher.find()
                ? new BigDecimal(matcher.group(1).replace(',', '.'))
                : null;
    }


    private Integer calcularDesconto(
            BigDecimal avaliacao,
            BigDecimal lanceMinimo
    ) {

        if (avaliacao == null
                || lanceMinimo == null
                || avaliacao.signum() <= 0) {
            return null;
        }

        BigDecimal percentual = lanceMinimo
                .multiply(BigDecimal.valueOf(100))
                .divide(avaliacao, 2, RoundingMode.HALF_UP);

        return Math.max(
                0,
                100 - percentual.setScale(0, RoundingMode.HALF_UP).intValue()
        );
    }


    private BigDecimal monetario(
            String valor
    ) {

        return new BigDecimal(
                valor.replace(".", "").replace(',', '.')
        );
    }


    private Document buscarPaginaHttp(
            String caminhoOuUrl
    ) throws Exception {

        String url = caminhoOuUrl.startsWith("http")
                ? caminhoOuUrl
                : URL_BASE + caminhoOuUrl;

        return Jsoup.connect(url)
                .userAgent(
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                                + "AppleWebKit/537.36 Chrome/128 Safari/537.36"
                )
                .header("Accept-Language", "pt-BR,pt;q=0.9")
                .referrer(URL_BASE + "/")
                .timeout(timeoutMs)
                .maxBodySize(0)
                .get();
    }


    private String texto(
            Element elemento
    ) {

        return elemento == null
                ? ""
                : elemento.text().replaceAll("\\s+", " ").trim();
    }


    private String normalizar(
            String texto
    ) {

        return Normalizer.normalize(
                        texto == null ? "" : texto,
                        Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }


    private String capitalizar(
            String valor
    ) {

        String normalizado = normalizar(valor);

        return "votorantim".equals(normalizado)
                ? "Votorantim"
                : "Sorocaba";
    }


    private String removerBarraFinal(
            String caminho
    ) {

        return caminho != null
                && caminho.length() > 1
                && caminho.endsWith("/")
                ? caminho.substring(0, caminho.length() - 1)
                : caminho;
    }


    private String limitarTexto(
            String texto,
            int limite
    ) {

        return texto.length() > limite
                ? texto.substring(0, limite)
                : texto;
    }


    private String valorOuNulo(
            String valor
    ) {

        return valor == null || valor.isBlank()
                ? null
                : valor;
    }


    @FunctionalInterface
    interface CarregadorPagina {

        Document carregar(
                String caminhoOuUrl
        ) throws Exception;
    }


    private record EtapaLeilao(
            int ordem,
            LocalDateTime data,
            BigDecimal valor,
            Integer percentual
    ) {
    }


    private record EnderecoZuk(
            String logradouro,
            String numero,
            String bairro,
            String cidade
    ) {
    }
}
