package br.com.bossolani.judicialpipeline.integration.leiloeiro.spy;

import br.com.bossolani.judicialpipeline.exception.LoteDescartadoException;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.ColetaLeiloeiroDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.LeiloeiroProvider;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.ResilienciaFonteService;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.DadosDinamicosLeilaoDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteDescobertoDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteLeilaoDTO;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
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
@Order(30)
public class SpyLeiloesProvider
        implements LeiloeiroProvider {

    private static final String NOME =
            "SPY Leilões";

    private static final String DOMINIO =
            "spyleiloes.com.br";

    private static final String URL_BASE =
            "https://spyleiloes.com.br";

    private static final Pattern CAMINHO_LOTE =
            Pattern.compile(
                    "^/leilao/\\d+/[^/]+/?$",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern PAGINA =
            Pattern.compile(
                    "(?:[?&])page=(\\d+)",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern NUMERO_PROCESSO =
            Pattern.compile(
                    "(?i)processo\\s*(?:n[º°o.]*)?\\s*[:\\-]?\\s*"
                            + "(\\d{7}-\\d{2}\\.\\d{4}\\.\\d\\.\\d{2}\\.\\d{4}|\\d{20})"
            );

    private static final Pattern VALOR_MONETARIO =
            Pattern.compile(
                    "R\\$\\s*([\\d.]+,\\d{2})"
            );

    private static final Pattern LINHA_PRACA =
            Pattern.compile(
                    "(\\d{2}/\\d{2}/\\d{4}\\s+\\d{2}:\\d{2}).*?"
                            + "(?:R\\$\\s*([\\d.]+,\\d{2})|Valor indisponível)",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern PERCENTUAL =
            Pattern.compile(
                    "Desconto\\s*(\\d{1,3})\\s*%",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern COMISSAO =
            Pattern.compile(
                    "(?i)comiss[aã]o.{0,80}?(\\d+(?:[.,]\\d+)?)\\s*%"
            );

    private static final Pattern VARA =
            Pattern.compile(
                    "(?i)(\\d+ª?\\s+Vara\\s+[^().]{0,80})"
            );

    private static final Pattern TIPO_TITULO =
            Pattern.compile(
                    "(?i)leil[aã]o\\s+de\\s+(.+?)\\s+em\\s+"
            );

    private static final DateTimeFormatter FORMATADOR_DATA =
            DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy HH:mm",
                    Locale.forLanguageTag("pt-BR")
            );

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper();

    private static final List<CidadeSpy> CIDADES =
            List.of(
                    new CidadeSpy(
                            "Sorocaba",
                            "/imoveis-leilao/sp/sorocaba/modalidade/judicial"
                    ),
                    new CidadeSpy(
                            "Votorantim",
                            "/imoveis-leilao/sp/votorantim/modalidade/judicial"
                    )
            );

    private final ResilienciaFonteService resiliencia;
    private final int timeoutMs;
    private final CarregadorPagina carregadorPagina;
    private final Clock clock;

    @Autowired
    public SpyLeiloesProvider(
            ResilienciaFonteService resiliencia,
            @Value("${integracao.fontes.timeout-ms:15000}")
            int timeoutMs
    ) {

        this(
                resiliencia,
                timeoutMs,
                null,
                Clock.systemDefaultZone()
        );
    }

    SpyLeiloesProvider(
            ResilienciaFonteService resiliencia,
            int timeoutMs,
            CarregadorPagina carregadorPagina,
            Clock clock
    ) {

        this.resiliencia = resiliencia;
        this.timeoutMs = Math.max(1000, timeoutMs);
        this.carregadorPagina =
                carregadorPagina != null
                        ? carregadorPagina
                        : this::buscarPaginaHttp;
        this.clock =
                clock != null
                        ? clock
                        : Clock.systemDefaultZone();
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

        String host =
                uri.getHost().toLowerCase(Locale.ROOT);

        return (host.equals(DOMINIO)
                || host.endsWith("." + DOMINIO))
                && CAMINHO_LOTE.matcher(uri.getPath()).matches();
    }

    @Override
    public String normalizarUrl(
            URI uri
    ) {

        if (!suporta(uri)) {
            throw new IllegalArgumentException(
                    "URL de lote da SPY Leilões inválida"
            );
        }

        try {
            return new URI(
                    "https",
                    null,
                    DOMINIO,
                    -1,
                    removerBarraFinal(uri.getPath()),
                    null,
                    null
            ).toASCIIString();

        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException(
                    "URL de lote da SPY Leilões inválida",
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

        Map<String, LoteDescobertoDTO> lotesPorUrl =
                new LinkedHashMap<>();

        for (CidadeSpy cidade : CIDADES) {
            descobrirCidade(cidade, lotesPorUrl);
        }

        return new ArrayList<>(lotesPorUrl.values());
    }

    private void descobrirCidade(
            CidadeSpy cidade,
            Map<String, LoteDescobertoDTO> lotesPorUrl
    ) throws Exception {

        int paginaAtual = 1;
        int totalPaginas = 1;

        do {
            String urlListagem =
                    URL_BASE
                            + cidade.caminho()
                            + (paginaAtual > 1
                            ? "?page=" + paginaAtual
                            : "");

            Document documento =
                    carregadorPagina.carregar(urlListagem);

            totalPaginas = Math.max(
                    totalPaginas,
                    calcularTotalPaginas(documento)
            );

            for (Element link : documento.select(
                    "a[href^=/leilao/]"
            )) {
                String href = link.absUrl("href");

                if (href.isBlank()) {
                    href = URL_BASE + link.attr("href");
                }

                URI uri;

                try {
                    uri = URI.create(href);

                } catch (IllegalArgumentException exception) {
                    continue;
                }

                if (!suporta(uri)) {
                    continue;
                }

                String urlNormalizada = normalizarUrl(uri);
                String titulo = link.attr("title").trim();

                if (titulo.isBlank()) {
                    titulo = texto(link.selectFirst("h2"));
                }

                LoteDescobertoDTO lote =
                        new LoteDescobertoDTO(
                                urlNormalizada,
                                titulo,
                                cidade.nome(),
                                link.text(),
                                NOME
                        );

                lotesPorUrl.putIfAbsent(
                        urlNormalizada,
                        lote
                );
            }

            paginaAtual++;

        } while (paginaAtual <= totalPaginas);
    }

    private int calcularTotalPaginas(
            Document documento
    ) {

        int total = 1;

        for (Element link : documento.select("a[href*=\"?page=\"]")) {
            Matcher matcher = PAGINA.matcher(link.attr("href"));

            if (matcher.find()) {
                total = Math.max(
                        total,
                        Integer.parseInt(matcher.group(1))
                );
            }
        }

        return total;
    }

    private ColetaLeiloeiroDTO coletarSemRetentativa(
            String url
    ) throws Exception {

        String urlNormalizada =
                normalizarUrl(URI.create(url));

        Document documento =
                carregadorPagina.carregar(urlNormalizada);

        String titulo = texto(documento.selectFirst("h1"));

        if (titulo.isBlank()) {
            throw new IllegalArgumentException(
                    "A SPY Leilões não retornou o título do imóvel"
            );
        }

        DadosEstruturados dadosEstruturados =
                extrairDadosEstruturados(documento);

        if (dadosEstruturados.cidade() == null
                || dadosEstruturados.cidade().isBlank()) {
            throw new IllegalArgumentException(
                    "A SPY Leilões não retornou a cidade do imóvel"
            );
        }

        String textoPagina = documento.body().text();
        String numeroProcesso = extrairNumeroProcesso(textoPagina);
        EnderecoSpy endereco = separarEndereco(
                dadosEstruturados.endereco(),
                dadosEstruturados.cidade()
        );

        Element blocoDatas = documento.selectFirst(
                "div[class^=auctionPage_datesDiv]"
        );

        BigDecimal avaliacao = extrairValor(
                texto(documento.selectFirst(
                        "span[class^=auctionPage_avalValue]"
                ))
        );

        List<PracaSpy> pracas = extrairPracas(blocoDatas);
        PracaSpy primeira =
                pracas.isEmpty()
                        ? null
                        : pracas.getFirst();
        PracaSpy segunda =
                pracas.size() > 1
                        ? pracas.get(1)
                        : null;

        BigDecimal lanceAtual = extrairValor(
                texto(documento.selectFirst(
                        "span[class^=auctionPage_h4LanceInicial]"
                ))
        );

        BigDecimal lanceMinimo = menorValor(
                primeira != null ? primeira.valor() : null,
                segunda != null ? segunda.valor() : null,
                lanceAtual
        );

        Integer desconto = extrairInteiro(
                blocoDatas != null
                        ? blocoDatas.text()
                        : textoPagina,
                PERCENTUAL
        );

        LocalDateTime encerramentoFinal =
                segunda != null
                        ? segunda.data()
                        : primeira != null
                        ? primeira.data()
                        : null;

        String status = definirStatus(
                textoPagina,
                encerramentoFinal
        );

        String resultado = definirResultado(textoPagina);

        LoteLeilaoDTO lote =
                new LoteLeilaoDTO(
                        numeroProcesso,
                        avaliacao,
                        dadosEstruturados.cidade(),
                        extrairVara(textoPagina),
                        inferirTipo(
                                titulo,
                                dadosEstruturados.tipo()
                        ),
                        endereco.logradouro(),
                        endereco.numero(),
                        endereco.bairro(),
                        urlNormalizada
                );

        DadosDinamicosLeilaoDTO leilao =
                new DadosDinamicosLeilaoDTO(
                        null,
                        primeira != null ? primeira.data() : null,
                        primeira != null ? primeira.valor() : null,
                        null,
                        segunda != null ? segunda.data() : null,
                        segunda != null ? segunda.valor() : null,
                        desconto,
                        status,
                        resultado,
                        lanceMinimo,
                        null,
                        extrairComissao(textoPagina)
                );

        return new ColetaLeiloeiroDTO(lote, leilao);
    }

    private DadosEstruturados extrairDadosEstruturados(
            Document documento
    ) {

        for (Element script : documento.select(
                "script[type=application/ld+json]"
        )) {
            String json = script.data();

            if (json.isBlank()
                    || !json.contains("RealEstateListing")) {
                continue;
            }

            try {
                JsonNode raiz = OBJECT_MAPPER.readTree(json);

                if (!"RealEstateListing".equals(
                        raiz.path("@type").asText()
                )) {
                    continue;
                }

                JsonNode entidade = raiz.path("mainEntity");
                JsonNode endereco = entidade.path("address");

                return new DadosEstruturados(
                        endereco.path("streetAddress").asText(null),
                        endereco.path("addressLocality").asText(null),
                        entidade.path("@type").asText(null)
                );

            } catch (Exception exception) {
                throw new IllegalArgumentException(
                        "Dados estruturados inválidos na página da SPY Leilões",
                        exception
                );
            }
        }

        throw new IllegalArgumentException(
                "A SPY Leilões não retornou os dados estruturados do imóvel"
        );
    }

    private List<PracaSpy> extrairPracas(
            Element blocoDatas
    ) {

        List<PracaSpy> pracas = new ArrayList<>();

        if (blocoDatas == null) {
            return pracas;
        }

        for (Element linha : blocoDatas.select(
                "span[class^=auctionPage_h6Value]"
        )) {
            Matcher matcher = LINHA_PRACA.matcher(linha.text());

            if (!matcher.find()) {
                continue;
            }

            LocalDateTime data = parseData(matcher.group(1));
            BigDecimal valor = extrairValor("R$ " + matcher.group(2));

            pracas.add(new PracaSpy(data, valor));
        }

        return pracas;
    }

    private String extrairNumeroProcesso(
            String texto
    ) {

        Matcher matcher = NUMERO_PROCESSO.matcher(texto);

        if (!matcher.find()) {
            throw new LoteDescartadoException(
                    "Anúncio judicial sem número de processo CNJ público; não é possível deduplicar e acompanhar com segurança."
            );
        }

        return matcher.group(1);
    }

    private String extrairVara(
            String texto
    ) {

        Matcher matcher = VARA.matcher(texto);

        return matcher.find()
                ? matcher.group(1).trim()
                : null;
    }

    private String inferirTipo(
            String titulo,
            String tipoEstruturado
    ) {

        Matcher matcher = TIPO_TITULO.matcher(titulo);

        if (matcher.find()) {
            return capitalizar(matcher.group(1));
        }

        if (tipoEstruturado != null
                && !tipoEstruturado.isBlank()) {
            return switch (tipoEstruturado) {
                case "Apartment" -> "Apartamento";
                case "House" -> "Casa";
                default -> capitalizar(tipoEstruturado);
            };
        }

        return "Imóvel";
    }

    private EnderecoSpy separarEndereco(
            String enderecoCompleto,
            String cidade
    ) {

        if (enderecoCompleto == null
                || enderecoCompleto.isBlank()) {
            return new EnderecoSpy(null, null, null);
        }

        String semCidade = enderecoCompleto
                .replaceAll(
                        "(?i),?\\s*" + Pattern.quote(cidade)
                                + "\\s*[-,/]?\\s*(?:São Paulo|SP)?(?:,?\\s*Brasil)?$",
                        ""
                )
                .trim();

        String[] partes = semCidade.split("\\s*,\\s*");
        String logradouro = partes.length > 0
                ? partes[0].trim()
                : semCidade;
        String numero = partes.length > 1
                ? extrairNumeroEndereco(partes[1])
                : null;
        String bairro = partes.length > 2
                ? partes[partes.length - 1].trim()
                : null;

        if (bairro != null
                && bairro.equalsIgnoreCase(logradouro)) {
            bairro = null;
        }

        return new EnderecoSpy(
                valorOuNulo(logradouro),
                valorOuNulo(numero),
                valorOuNulo(bairro)
        );
    }

    private String extrairNumeroEndereco(
            String trecho
    ) {

        Matcher matcher = Pattern.compile(
                "(?i)(?:n[º°o.]?\\s*)?(\\d+[A-Za-z]?)"
        ).matcher(trecho);

        return matcher.find()
                ? matcher.group(1)
                : trecho.matches("(?i)s/?n")
                ? "s/n"
                : null;
    }

    private String definirStatus(
            String textoPagina,
            LocalDateTime encerramentoFinal
    ) {

        String normalizado = normalizar(textoPagina);

        if (normalizado.contains("leilao cancelado")) {
            return "CANCELADO";
        }

        if (normalizado.contains("leilao suspenso")) {
            return "SUSPENSO";
        }

        if (normalizado.contains("leilao encerrado")
                || encerramentoFinal != null
                && encerramentoFinal.isBefore(
                LocalDateTime.now(clock)
        )) {
            return "ENCERRADO";
        }

        return "AGENDADO";
    }

    private String definirResultado(
            String textoPagina
    ) {

        String normalizado = normalizar(textoPagina);

        if (normalizado.contains("sem lances")) {
            return "SEM LANCES";
        }

        if (normalizado.contains("arrematado")) {
            return "ARREMATADO";
        }

        return "DESCONHECIDO";
    }

    private BigDecimal extrairComissao(
            String texto
    ) {

        Matcher matcher = COMISSAO.matcher(texto);

        if (!matcher.find()) {
            return null;
        }

        return new BigDecimal(
                matcher.group(1).replace(',', '.')
        );
    }

    private Integer extrairInteiro(
            String texto,
            Pattern pattern
    ) {

        Matcher matcher = pattern.matcher(
                texto == null ? "" : texto
        );

        return matcher.find()
                ? Integer.parseInt(matcher.group(1))
                : null;
    }

    private BigDecimal extrairValor(
            String texto
    ) {

        if (texto == null) {
            return null;
        }

        Matcher matcher = VALOR_MONETARIO.matcher(texto);

        if (!matcher.find()) {
            return null;
        }

        return new BigDecimal(
                matcher.group(1)
                        .replace(".", "")
                        .replace(',', '.')
        );
    }

    private BigDecimal menorValor(
            BigDecimal... valores
    ) {

        BigDecimal menor = null;

        for (BigDecimal valor : valores) {
            if (valor != null
                    && (menor == null
                    || valor.compareTo(menor) < 0)) {
                menor = valor;
            }
        }

        return menor != null
                ? menor.setScale(2, RoundingMode.HALF_UP)
                : null;
    }

    private LocalDateTime parseData(
            String texto
    ) {

        try {
            return LocalDateTime.parse(
                    texto.trim(),
                    FORMATADOR_DATA
            );

        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    "Data de praça inválida na SPY Leilões: " + texto,
                    exception
            );
        }
    }

    private Document buscarPaginaHttp(
            String url
    ) throws Exception {

        return Jsoup.connect(url)
                .userAgent(
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                                + "AppleWebKit/537.36 Chrome/128 Safari/537.36"
                )
                .header("Accept-Language", "pt-BR,pt;q=0.9")
                .timeout(timeoutMs)
                .get();
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

    private String texto(
            Element elemento
    ) {

        return elemento == null
                ? ""
                : elemento.text().trim();
    }

    private String normalizar(
            String texto
    ) {

        return texto == null
                ? ""
                : java.text.Normalizer.normalize(
                                texto,
                                java.text.Normalizer.Form.NFD
                        )
                        .replaceAll("\\p{M}", "")
                        .toLowerCase(Locale.ROOT);
    }

    private String capitalizar(
            String texto
    ) {

        if (texto == null
                || texto.isBlank()) {
            return "Imóvel";
        }

        String limpo = texto.trim().toLowerCase(
                Locale.forLanguageTag("pt-BR")
        );

        return Character.toUpperCase(limpo.charAt(0))
                + limpo.substring(1);
    }

    private String valorOuNulo(
            String valor
    ) {

        return valor == null
                || valor.isBlank()
                ? null
                : valor.trim();
    }

    @FunctionalInterface
    interface CarregadorPagina {

        Document carregar(String url)
                throws Exception;
    }

    private record CidadeSpy(
            String nome,
            String caminho
    ) {
    }

    private record DadosEstruturados(
            String endereco,
            String cidade,
            String tipo
    ) {
    }

    private record PracaSpy(
            LocalDateTime data,
            BigDecimal valor
    ) {
    }

    private record EnderecoSpy(
            String logradouro,
            String numero,
            String bairro
    ) {
    }
}
