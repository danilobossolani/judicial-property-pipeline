package br.com.bossolani.judicialpipeline.integration.leiloeiro.mega;

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
import java.net.URI;
import java.net.URISyntaxException;
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
@Order(20)
public class MegaLeiloesProvider
        implements LeiloeiroProvider {

    private static final String NOME =
            "Mega Leilões";

    private static final String DOMINIO =
            "megaleiloes.com.br";

    private static final String URL_BASE =
            "https://www.megaleiloes.com.br";

    private static final Pattern CAMINHO_LOTE =
            Pattern.compile(
                    "^/imoveis/.+-j\\d+/?$",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern TOTAL_PAGINAS =
            Pattern.compile(
                    "Página\\s+\\d+\\s+de\\s+(\\d+)",
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

    private static final Pattern PERCENTUAL =
            Pattern.compile(
                    "(\\d+(?:[.,]\\d+)?)\\s*%"
            );

    private static final DateTimeFormatter FORMATADOR_DATA =
            DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy 'às' HH:mm",
                    Locale.forLanguageTag(
                            "pt-BR"
                    )
            );

    private static final List<String> CIDADES =
            List.of(
                    "Sorocaba",
                    "Votorantim"
            );

    private final ResilienciaFonteService resiliencia;

    private final int timeoutMs;

    private final CarregadorPagina carregadorPagina;


    @Autowired
    public MegaLeiloesProvider(
            ResilienciaFonteService resiliencia,
            @Value("${integracao.fontes.timeout-ms:15000}")
            int timeoutMs
    ) {

        this(
                resiliencia,
                timeoutMs,
                null
        );
    }


    MegaLeiloesProvider(
            ResilienciaFonteService resiliencia,
            int timeoutMs,
            CarregadorPagina carregadorPagina
    ) {

        this.resiliencia = resiliencia;
        this.timeoutMs = Math.max(
                1000,
                timeoutMs
        );
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
                || !"https".equalsIgnoreCase(
                uri.getScheme()
        )) {
            return false;
        }


        String host =
                uri.getHost()
                        .toLowerCase(
                                Locale.ROOT
                        );


        return (host.equals(
                DOMINIO
        )
                || host.endsWith(
                "." + DOMINIO
        ))
                && CAMINHO_LOTE.matcher(
                uri.getPath()
        ).matches();
    }


    @Override
    public String normalizarUrl(
            URI uri
    ) {

        if (!suporta(
                uri
        )) {

            throw new IllegalArgumentException(
                    "URL de lote da Mega Leilões inválida"
            );
        }


        String caminho =
                removerBarraFinal(
                        uri.getPath()
                );


        try {

            return new URI(
                    "https",
                    null,
                    uri.getHost()
                            .toLowerCase(
                                    Locale.ROOT
                            ),
                    -1,
                    caminho,
                    null,
                    null
            ).toASCIIString();

        } catch (URISyntaxException exception) {

            throw new IllegalArgumentException(
                    "URL de lote da Mega Leilões inválida",
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
                () -> coletarSemRetentativa(
                        url
                )
        );
    }


    private List<LoteDescobertoDTO> descobrirSemRetentativa()
            throws Exception {

        Map<String, LoteDescobertoDTO> lotesPorUrl =
                new LinkedHashMap<>();


        for (String cidade : CIDADES) {

            descobrirCidade(
                    cidade,
                    lotesPorUrl
            );
        }


        return new ArrayList<>(
                lotesPorUrl.values()
        );
    }


    private void descobrirCidade(
            String cidade,
            Map<String, LoteDescobertoDTO> lotesPorUrl
    ) throws Exception {

        int pagina = 1;
        int totalPaginas = 1;


        do {

            Document documento =
                    buscarPagina(
                            urlCidade(
                                    cidade,
                                    pagina
                            )
                    );


            totalPaginas =
                    calcularTotalPaginas(
                            documento
                    );


            for (Element cartao : documento.select(
                    ".card"
            )) {

                Element titulo =
                        cartao.selectFirst(
                                "a.card-title[href]"
                        );


                if (titulo == null) {
                    continue;
                }


                URI uri =
                        URI.create(
                                titulo.absUrl(
                                        "href"
                                )
                        );


                if (!suporta(
                        uri
                )) {
                    continue;
                }


                String urlNormalizada =
                        normalizarUrl(
                                uri
                        );


                String cidadeFonte =
                        texto(
                                cartao.selectFirst(
                                        ".card-locality"
                                )
                        )
                                .replaceAll(
                                        "(?i),\\s*SP$",
                                        ""
                                )
                                .trim();


                LoteDescobertoDTO lote =
                        new LoteDescobertoDTO(
                                urlNormalizada,
                                titulo.text()
                                        .trim(),
                                cidadeFonte.isBlank()
                                        ? cidade
                                        : cidadeFonte,
                                cartao.text(),
                                NOME
                        );


                lotesPorUrl.putIfAbsent(
                        urlNormalizada,
                        lote
                );
            }


            pagina++;

        } while (pagina <= totalPaginas);
    }


    private ColetaLeiloeiroDTO coletarSemRetentativa(
            String url
    ) throws Exception {

        URI uri =
                URI.create(
                        url
                );


        String urlNormalizada =
                normalizarUrl(
                        uri
                );


        Document documento =
                buscarPagina(
                        urlNormalizada
                );


        String titulo =
                texto(
                        documento.selectFirst(
                                "h1.section-header"
                        )
                );


        if (titulo.isBlank()) {

            throw new IllegalArgumentException(
                    "A Mega Leilões não retornou o título do lote"
            );
        }


        String localizacao =
                texto(
                        documento.selectFirst(
                                ".locality.item .value"
                        )
                );


        EnderecoMega endereco =
                separarEndereco(
                        localizacao
                );


        LoteLeilaoDTO lote =
                new LoteLeilaoDTO(
                        extrairNumeroProcesso(
                                texto(
                                        documento.selectFirst(
                                                ".process-number.item .value"
                                        )
                                )
                        ),
                        extrairValor(
                                texto(
                                        documento.selectFirst(
                                                ".rating-value.item .value"
                                        )
                                )
                        ),
                        endereco.cidade(),
                        texto(
                                documento.selectFirst(
                                        ".jurisdiction.item .value"
                                )
                        ),
                        inferirTipo(
                                titulo,
                                uri.getPath()
                        ),
                        endereco.logradouro(),
                        endereco.numero(),
                        endereco.bairro(),
                        urlNormalizada
                );


        DadosDinamicosLeilaoDTO leilao =
                extrairLeilao(
                        documento
                );


        return new ColetaLeiloeiroDTO(
                lote,
                leilao
        );
    }


    private DadosDinamicosLeilaoDTO extrairLeilao(
            Document documento
    ) {

        Element primeiraPraca =
                documento.selectFirst(
                        ".summary-info .instance.first"
                );

        Element segundaPraca =
                documento.selectFirst(
                        ".summary-info .instance:not(.first)"
                );


        LocalDateTime fechamento1 =
                extrairData(
                        primeiraPraca,
                        ".card-first-instance-date"
                );

        BigDecimal lance1 =
                extrairValor(
                        texto(
                                primeiraPraca == null
                                        ? null
                                        : primeiraPraca.selectFirst(
                                        ".card-instance-value"
                                )
                        )
                );


        LocalDateTime fechamento2 =
                extrairData(
                        segundaPraca,
                        ".card-second-instance-date"
                );

        BigDecimal lance2 =
                extrairValor(
                        texto(
                                segundaPraca == null
                                        ? null
                                        : segundaPraca.selectFirst(
                                        ".card-instance-value"
                                )
                        )
                );


        String status =
                texto(
                        documento.selectFirst(
                                ".summary-info .instance-text"
                        )
                );


        Integer desconto =
                extrairInteiro(
                        texto(
                                documento.selectFirst(
                                        ".defasagem .value"
                                )
                        )
                );


        BigDecimal lanceMinimo =
                extrairValor(
                        texto(
                                documento.selectFirst(
                                        ".summary-info .last-bid .value"
                                )
                        )
                );


        if (lanceMinimo == null) {

            lanceMinimo =
                    extrairValor(
                            texto(
                                    documento.selectFirst(
                                            ".price .value"
                                    )
                            )
                    );
        }


        BigDecimal incremento =
                extrairValor(
                        texto(
                                documento.selectFirst(
                                        ".summary-info .increment .value"
                                )
                        )
                );


        BigDecimal comissao =
                extrairComissao(
                        documento.text()
                );


        return new DadosDinamicosLeilaoDTO(
                null,
                fechamento1,
                lance1,
                null,
                fechamento2,
                lance2,
                desconto,
                status,
                inferirResultado(
                        status
                ),
                lanceMinimo,
                incremento,
                comissao
        );
    }


    private Document buscarPagina(
            String url
    ) throws Exception {

        return carregadorPagina.carregar(
                url
        );
    }


    private Document buscarPaginaHttp(
            String url
    ) throws Exception {

        return Jsoup.connect(
                        url
                )
                .userAgent(
                        "Mozilla/5.0 (compatible; JudicialPipeline/1.0; +monitoramento-de-leiloes)"
                )
                .referrer(
                        URL_BASE + "/"
                )
                .timeout(
                        timeoutMs
                )
                .maxBodySize(
                        0
                )
                .get();
    }


    private int calcularTotalPaginas(
            Document documento
    ) {

        String resumo =
                texto(
                        documento.selectFirst(
                                ".summary"
                        )
                );


        Matcher matcher =
                TOTAL_PAGINAS.matcher(
                        resumo
                );


        return matcher.find()
                ? Math.max(
                1,
                Integer.parseInt(
                        matcher.group(1)
                )
        )
                : 1;
    }


    private String urlCidade(
            String cidade,
            int pagina
    ) {

        String slug =
                cidade.toLowerCase(
                        Locale.ROOT
                );


        return URL_BASE
                + "/sp/"
                + slug
                + (pagina <= 1
                ? ""
                : "?pagina=" + pagina);
    }


    private String extrairNumeroProcesso(
            String texto
    ) {

        Matcher matcher =
                NUMERO_PROCESSO.matcher(
                        texto == null
                                ? ""
                                : texto
                );


        return matcher.find()
                ? matcher.group()
                : null;
    }


    private LocalDateTime extrairData(
            Element elemento,
            String seletor
    ) {

        if (elemento == null) {
            return null;
        }


        String data =
                texto(
                        elemento.selectFirst(
                                seletor
                        )
                )
                        .replaceFirst(
                                "(?i)^.*?Praça:\\s*",
                                ""
                        )
                        .trim();


        try {

            return LocalDateTime.parse(
                    data,
                    FORMATADOR_DATA
            );

        } catch (DateTimeParseException exception) {

            return null;
        }
    }


    private BigDecimal extrairValor(
            String texto
    ) {

        Matcher matcher =
                VALOR_MONETARIO.matcher(
                        texto == null
                                ? ""
                                : texto
                );


        if (!matcher.find()) {
            return null;
        }


        return new BigDecimal(
                matcher.group(1)
                        .replace(
                                ".",
                                ""
                        )
                        .replace(
                                ",",
                                "."
                        )
        );
    }


    private Integer extrairInteiro(
            String texto
    ) {

        Matcher matcher =
                PERCENTUAL.matcher(
                        texto == null
                                ? ""
                                : texto
                );


        if (!matcher.find()) {
            return null;
        }


        return new BigDecimal(
                matcher.group(1)
                        .replace(
                                ",",
                                "."
                        )
        ).intValue();
    }


    private BigDecimal extrairComissao(
            String texto
    ) {

        Pattern padrao =
                Pattern.compile(
                        "(?i)comiss[aã]o.{0,120}?(\\d+(?:[.,]\\d+)?)\\s*%"
                );

        Matcher matcher =
                padrao.matcher(
                        texto == null
                                ? ""
                                : texto
                );


        return matcher.find()
                ? new BigDecimal(
                matcher.group(1)
                        .replace(
                                ",",
                                "."
                        )
        )
                : null;
    }


    private String inferirResultado(
            String status
    ) {

        String normalizado =
                status == null
                        ? ""
                        : status.toLowerCase(
                        Locale.ROOT
                );


        if (normalizado.contains(
                "arrematado"
        )) {
            return "ARREMATADO";
        }


        if (normalizado.contains(
                "sem lance"
        )) {
            return "SEM LANCES";
        }


        if (normalizado.contains(
                "deserto"
        )) {
            return "DESERTO";
        }


        if (normalizado.contains(
                "com lance"
        )) {
            return "COM LANCES";
        }


        return "DESCONHECIDO";
    }


    private String inferirTipo(
            String titulo,
            String caminho
    ) {

        String texto =
                (titulo + " " + caminho)
                        .toLowerCase(
                                Locale.ROOT
                        );


        Map<String, String> tipos =
                new LinkedHashMap<>();

        tipos.put(
                "apartamento",
                "Apartamento"
        );
        tipos.put(
                "casa",
                "Casa"
        );
        tipos.put(
                "terreno",
                "Terreno"
        );
        tipos.put(
                "galp",
                "Galpão"
        );
        tipos.put(
                "industrial",
                "Imóvel industrial"
        );
        tipos.put(
                "comercial",
                "Imóvel comercial"
        );
        tipos.put(
                "rural",
                "Imóvel rural"
        );


        return tipos.entrySet()
                .stream()
                .filter(entrada ->
                        texto.contains(
                                entrada.getKey()
                        )
                )
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(
                        "Imóvel"
                );
    }


    private EnderecoMega separarEndereco(
            String localizacao
    ) {

        if (localizacao == null
                || localizacao.isBlank()) {

            return new EnderecoMega(
                    null,
                    null,
                    null,
                    null
            );
        }


        String[] partes =
                localizacao.split(
                        "\\s*,\\s*"
                );


        if (partes.length < 4) {

            return new EnderecoMega(
                    localizacao,
                    null,
                    null,
                    identificarCidade(
                            localizacao
                    )
            );
        }


        int indiceCidade =
                partes.length - 2;

        int indiceBairro =
                partes.length - 3;

        int indiceNumero =
                partes.length - 4;


        StringBuilder logradouro =
                new StringBuilder();


        for (int indice = 0;
             indice < indiceNumero;
             indice++) {

            if (!logradouro.isEmpty()) {
                logradouro.append(
                        ", "
                );
            }


            logradouro.append(
                    partes[indice]
            );
        }


        return new EnderecoMega(
                logradouro.toString(),
                partes[indiceNumero],
                partes[indiceBairro],
                partes[indiceCidade]
        );
    }


    private String identificarCidade(
            String texto
    ) {

        String normalizado =
                texto.toLowerCase(
                        Locale.ROOT
                );


        return CIDADES.stream()
                .filter(cidade ->
                        normalizado.contains(
                                cidade.toLowerCase(
                                        Locale.ROOT
                                )
                        )
                )
                .findFirst()
                .orElse(null);
    }


    private String texto(
            Element elemento
    ) {

        return elemento == null
                ? ""
                : elemento.text()
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }


    private String removerBarraFinal(
            String caminho
    ) {

        if (caminho == null
                || caminho.isBlank()
                || "/".equals(
                caminho
        )) {
            return caminho;
        }


        return caminho.endsWith("/")
                ? caminho.substring(
                0,
                caminho.length() - 1
        )
                : caminho;
    }


    private record EnderecoMega(
            String logradouro,
            String numero,
            String bairro,
            String cidade
    ) {
    }


    @FunctionalInterface
    interface CarregadorPagina {

        Document carregar(
                String url
        ) throws Exception;
    }
}
