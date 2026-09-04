package br.com.bossolani.judicialpipeline.integration.leiloeiro.publicjud;

import br.com.bossolani.judicialpipeline.integration.leiloeiro.ColetaLeiloeiroDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.LeiloeiroProvider;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.ResilienciaFonteService;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.DadosDinamicosLeilaoDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteDescobertoDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteLeilaoDTO;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class PublicJudProvider
        implements LeiloeiroProvider {

    private static final Logger log =
            LoggerFactory.getLogger(PublicJudProvider.class);

    private static final String NOME =
            "PublicJud (editais judiciais)";

    private static final String DOMINIO =
            "publicjud.com.br";

    private static final String URL_BASE =
            "https://www.publicjud.com.br";

    private static final Pattern CAMINHO_EDITAL =
            Pattern.compile(
                    "^/edital/\\d+/?$",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern NUMERO_PROCESSO =
            Pattern.compile(
                    "(?i)(?:processo\\s*(?:n[º°o.]*)?\\s*[:.\\-]?\\s*)?"
                            + "(\\d{7}-\\d{2}\\.\\d{4}\\.\\d\\.\\d{2}\\.\\d{4})"
            );

    private static final Pattern EDITAL_MULTIPLOS_LOTES =
            Pattern.compile(
                    "(?i)(?:^|\\s)\\d+\\s*-\\s*Processo\\s*:"
            );

    private static final Pattern VALOR_AVALIACAO_ATUALIZADO =
            Pattern.compile(
                    "(?i)valor\\s+de\\s+avalia[cç][aã]o\\s+atualizado\\s*:\\s*"
                            + "R\\$\\s*([\\d.]+,\\d{2})"
            );

    private static final Pattern VALOR_AVALIACAO =
            Pattern.compile(
                    "(?i)(?:valor\\s+de\\s+)?avalia[cç][aã]o(?:\\s*\\([^)]*\\))?\\s*:\\s*"
                            + "R\\$\\s*([\\d.]+,\\d{2})"
            );

    private static final Pattern PERCENTUAL_LANCE =
            Pattern.compile(
                    "(?i)(?:inferior\\s+a|correspondente\\s+a|lance\\s+m[ií]nimo(?:\\s+de)?)"
                            + "\\s*(\\d{1,3})\\s*%"
            );

    private static final Pattern COMISSAO =
            Pattern.compile(
                    "(?i)comiss[aã]o.{0,100}?(\\d+(?:[.,]\\d+)?)\\s*%"
            );

    private static final Pattern LOCALIZACAO =
            Pattern.compile(
                    "(?i)(?:localiza[cç][aã]o|endere[cç]o\\s+do\\s+im[oó]vel)\\s*:\\s*"
                            + "(.{5,260}?)(?=(?:visita[cç][aã]o|matr[ií]cula|avalia[cç][aã]o|\\.\\s+[A-ZÁÉÍÓÚÂÊÔÃÕÇ]{3,})|$)"
            );

    private static final Pattern IMOVEL_SITUADO =
            Pattern.compile(
                    "(?i)(?:im[oó]vel|apartamento|casa|terreno).{0,80}?situad[oa]\\s+"
                            + "(?:na|no|à|ao)\\s+(.{5,240}?(?:Sorocaba|Votorantim)[^.;]{0,60})"
            );

    private static final DateTimeFormatter FORMATADOR_DATA =
            DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy HH:mm:ss",
                    Locale.forLanguageTag("pt-BR")
            );

    private static final List<CidadePublicJud> CIDADES =
            List.of(
                    new CidadePublicJud("Sorocaba", "9450"),
                    new CidadePublicJud("Votorantim", "9542")
            );

    private final ResilienciaFonteService resiliencia;
    private final int timeoutMs;
    private final CarregadorPagina carregadorPagina;
    private final Clock clock;

    @Autowired
    public PublicJudProvider(
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

    PublicJudProvider(
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

        String host = uri.getHost().toLowerCase(Locale.ROOT);

        return (host.equals(DOMINIO)
                || host.endsWith("." + DOMINIO))
                && CAMINHO_EDITAL.matcher(uri.getPath()).matches();
    }

    @Override
    public String normalizarUrl(
            URI uri
    ) {

        if (!suporta(uri)) {
            throw new IllegalArgumentException(
                    "URL de edital do PublicJud inválida"
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
                    "URL de edital do PublicJud inválida",
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

        for (CidadePublicJud cidade : CIDADES) {
            Document listagem = carregadorPagina.carregar(
                    URL_BASE + "/consulta",
                    Map.of(
                            "data[Diario][cidade_id]", cidade.id(),
                            "data[Diario][estado]", "26",
                            "data[Diario][periodo_leilao]", "1",
                            "data[Diario][termo]", "",
                            "data[Diario][processo]", ""
                    )
            );

            for (Element link : listagem.select(
                    "a[href^=/edital/]"
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

                if (lotesPorUrl.containsKey(urlNormalizada)) {
                    continue;
                }

                try {
                    Document detalhe = carregadorPagina.carregar(
                            urlNormalizada,
                            Map.of()
                    );

                    String conteudo = extrairConteudo(detalhe);
                    String cidadeImovel = extrairCidadeImovel(
                            detalhe,
                            conteudo
                    );

                    if (!editalUnitarioDeImovel(
                            conteudo,
                            cidadeImovel
                    )) {
                        continue;
                    }

                    String tipo = inferirTipo(conteudo);
                    String codigo = valorTabela(detalhe, "Código");

                    lotesPorUrl.put(
                            urlNormalizada,
                            new LoteDescobertoDTO(
                                    urlNormalizada,
                                    tipo + " em " + cidadeImovel
                                            + (codigo.isBlank()
                                            ? ""
                                            : " - edital " + codigo),
                                    cidadeImovel,
                                    limitarTexto(conteudo, 4000),
                                    NOME
                            )
                    );

                } catch (Exception exception) {
                    log.warn(
                            "Edital do PublicJud ignorado por não oferecer dados estruturados suficientes: {} ({})",
                            urlNormalizada,
                            exception.getMessage()
                    );
                }
            }
        }

        return new ArrayList<>(lotesPorUrl.values());
    }

    private ColetaLeiloeiroDTO coletarSemRetentativa(
            String url
    ) throws Exception {

        String urlNormalizada =
                normalizarUrl(URI.create(url));

        Document documento = carregadorPagina.carregar(
                urlNormalizada,
                Map.of()
        );

        String conteudo = extrairConteudo(documento);
        String cidade = extrairCidadeImovel(documento, conteudo);

        if (!editalUnitarioDeImovel(conteudo, cidade)) {
            throw new IllegalArgumentException(
                    "Edital do PublicJud sem um único imóvel elegível na região"
            );
        }

        String numeroProcesso = extrairNumeroProcesso(conteudo);
        String vara = valorTabela(documento, "Vara");
        String primeiroLeilao = valorTabela(
                documento,
                "Primeiro Leilão"
        );
        String ultimoLeilao = valorTabela(
                documento,
                "Último Leilão"
        );
        LocalDateTime dataPrimeiro = parseData(primeiroLeilao);
        LocalDateTime dataUltimo = parseData(ultimoLeilao);
        BigDecimal avaliacao = extrairAvaliacao(conteudo);
        Integer percentualMinimo = extrairPercentualMinimo(conteudo);
        BigDecimal lanceSegunda = calcularLance(
                avaliacao,
                percentualMinimo
        );
        Integer desconto = percentualMinimo != null
                ? Math.max(0, 100 - percentualMinimo)
                : null;
        EnderecoPublicJud endereco = separarEndereco(
                extrairEndereco(conteudo),
                cidade
        );

        LoteLeilaoDTO lote =
                new LoteLeilaoDTO(
                        numeroProcesso,
                        avaliacao,
                        cidade,
                        vara.isBlank() ? null : vara,
                        inferirTipo(conteudo),
                        endereco.logradouro(),
                        endereco.numero(),
                        endereco.bairro(),
                        urlNormalizada
                );

        DadosDinamicosLeilaoDTO leilao =
                new DadosDinamicosLeilaoDTO(
                        dataPrimeiro,
                        dataPrimeiro,
                        avaliacao,
                        null,
                        dataUltimo,
                        lanceSegunda,
                        desconto,
                        definirStatus(
                                valorTabela(documento, "Situação"),
                                dataUltimo
                        ),
                        "DESCONHECIDO",
                        lanceSegunda != null
                                ? lanceSegunda
                                : avaliacao,
                        null,
                        extrairComissao(conteudo)
                );

        return new ColetaLeiloeiroDTO(lote, leilao);
    }

    private boolean editalUnitarioDeImovel(
            String conteudo,
            String cidade
    ) {

        if (conteudo == null
                || conteudo.isBlank()
                || cidade == null
                || cidade.isBlank()) {
            return false;
        }

        Matcher multiplos = EDITAL_MULTIPLOS_LOTES.matcher(conteudo);
        int totalMarcadores = 0;

        while (multiplos.find()) {
            totalMarcadores++;
        }

        if (totalMarcadores > 1) {
            return false;
        }

        String normalizado = normalizar(conteudo);

        return contemIndicadorImovel(normalizado)
                && normalizado.contains(normalizar(cidade));
    }

    private boolean contemIndicadorImovel(
            String texto
    ) {

        return List.of(
                        "imovel",
                        "apartamento",
                        "casa",
                        "terreno",
                        "gleba",
                        "galpao",
                        "predio",
                        "area rural",
                        "area industrial"
                )
                .stream()
                .anyMatch(texto::contains);
    }

    private String extrairConteudo(
            Document documento
    ) {

        Element valor = celulaAposRotulo(
                documento,
                "Conteúdo"
        );

        return valor == null
                ? ""
                : valor.text().replaceAll("\\s+", " ").trim();
    }

    private String extrairCidadeImovel(
            Document documento,
            String conteudo
    ) {

        String cidadeMetadados = valorTabela(
                documento,
                "Cidade/UF"
        ).replaceAll("(?i)/SP$", "").trim();

        String normalizado = normalizar(conteudo);

        if ("Sorocaba".equalsIgnoreCase(cidadeMetadados)
                && normalizado.contains("sorocaba")) {
            return "Sorocaba";
        }

        if ("Votorantim".equalsIgnoreCase(cidadeMetadados)
                && normalizado.contains("votorantim")) {
            return "Votorantim";
        }

        return null;
    }

    private String extrairNumeroProcesso(
            String conteudo
    ) {

        Matcher matcher = NUMERO_PROCESSO.matcher(conteudo);

        if (!matcher.find()) {
            throw new IllegalArgumentException(
                    "Edital do PublicJud sem número de processo CNJ"
            );
        }

        return matcher.group(1);
    }

    private BigDecimal extrairAvaliacao(
            String conteudo
    ) {

        BigDecimal atualizado = extrairValor(
                conteudo,
                VALOR_AVALIACAO_ATUALIZADO
        );

        return atualizado != null
                ? atualizado
                : extrairValor(conteudo, VALOR_AVALIACAO);
    }

    private Integer extrairPercentualMinimo(
            String conteudo
    ) {

        Matcher matcher = PERCENTUAL_LANCE.matcher(conteudo);

        if (!matcher.find()) {
            return null;
        }

        int percentual = Integer.parseInt(matcher.group(1));

        return percentual > 0
                && percentual <= 100
                ? percentual
                : null;
    }

    private BigDecimal calcularLance(
            BigDecimal avaliacao,
            Integer percentualMinimo
    ) {

        if (avaliacao == null
                || percentualMinimo == null) {
            return null;
        }

        return avaliacao
                .multiply(BigDecimal.valueOf(percentualMinimo))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal extrairComissao(
            String conteudo
    ) {

        Matcher matcher = COMISSAO.matcher(conteudo);

        return matcher.find()
                ? new BigDecimal(
                matcher.group(1).replace(',', '.')
        )
                : null;
    }

    private String extrairEndereco(
            String conteudo
    ) {

        Matcher localizacao = LOCALIZACAO.matcher(conteudo);

        if (localizacao.find()) {
            return localizacao.group(1).trim();
        }

        Matcher situado = IMOVEL_SITUADO.matcher(conteudo);

        return situado.find()
                ? situado.group(1).trim()
                : null;
    }

    private EnderecoPublicJud separarEndereco(
            String enderecoCompleto,
            String cidade
    ) {

        if (enderecoCompleto == null
                || enderecoCompleto.isBlank()) {
            return new EnderecoPublicJud(
                    "Endereço informado no edital",
                    null,
                    null
            );
        }

        String semCidade = enderecoCompleto
                .replaceAll(
                        "(?i),?\\s*" + Pattern.quote(cidade)
                                + "\\s*[-/]?\\s*SP(?:,?\\s*\\d{5}-?\\d{3})?.*$",
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

        return new EnderecoPublicJud(
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

    private String inferirTipo(
            String conteudo
    ) {

        String normalizado = normalizar(conteudo);

        if (normalizado.contains("apartamento")) {
            return "Apartamento";
        }

        if (normalizado.contains("galpao")
                || normalizado.contains("imovel industrial")) {
            return "Galpão";
        }

        if (normalizado.contains("terreno")
                || normalizado.contains("gleba")) {
            return "Terreno";
        }

        if (normalizado.contains("casa")) {
            return "Casa";
        }

        if (normalizado.contains("predio")) {
            return "Prédio";
        }

        return "Imóvel";
    }

    private String definirStatus(
            String situacao,
            LocalDateTime encerramentoFinal
    ) {

        String normalizado = normalizar(situacao);

        if (normalizado.contains("cancelad")) {
            return "CANCELADO";
        }

        if (normalizado.contains("suspens")) {
            return "SUSPENSO";
        }

        if (encerramentoFinal != null
                && encerramentoFinal.isBefore(
                LocalDateTime.now(clock)
        )) {
            return "ENCERRADO";
        }

        return "AGENDADO";
    }

    private String valorTabela(
            Document documento,
            String rotulo
    ) {

        Element valor = celulaAposRotulo(documento, rotulo);

        return valor == null
                ? ""
                : valor.text().trim();
    }

    private Element celulaAposRotulo(
            Document documento,
            String rotulo
    ) {

        String esperado = normalizar(rotulo).trim();

        for (Element cabecalho : documento.select("th")) {
            if (normalizar(cabecalho.text()).trim().equals(esperado)) {
                Element proximo = cabecalho.nextElementSibling();

                if (proximo != null
                        && "td".equals(proximo.tagName())) {
                    return proximo;
                }
            }
        }

        return null;
    }

    private BigDecimal extrairValor(
            String texto,
            Pattern pattern
    ) {

        Matcher matcher = pattern.matcher(texto);

        if (!matcher.find()) {
            return null;
        }

        return new BigDecimal(
                matcher.group(1)
                        .replace(".", "")
                        .replace(',', '.')
        );
    }

    private LocalDateTime parseData(
            String texto
    ) {

        if (texto == null
                || texto.isBlank()) {
            return null;
        }

        try {
            return LocalDateTime.parse(
                    texto.trim(),
                    FORMATADOR_DATA
            );

        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    "Data de leilão inválida no PublicJud: " + texto,
                    exception
            );
        }
    }

    private Document buscarPaginaHttp(
            String url,
            Map<String, String> formulario
    ) throws Exception {

        Connection conexao = Jsoup.connect(url)
                .userAgent(
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                                + "AppleWebKit/537.36 Chrome/128 Safari/537.36"
                )
                .header("Accept-Language", "pt-BR,pt;q=0.9")
                .timeout(timeoutMs);

        if (formulario != null
                && !formulario.isEmpty()) {
            formulario.forEach(conexao::data);
            return conexao.method(Connection.Method.POST).post();
        }

        return conexao.get();
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

    private String normalizar(
            String texto
    ) {

        if (texto == null) {
            return "";
        }

        return Normalizer.normalize(
                        texto,
                        Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private String valorOuNulo(
            String valor
    ) {

        return valor == null
                || valor.isBlank()
                ? null
                : valor.trim();
    }

    private String limitarTexto(
            String texto,
            int limite
    ) {

        return texto.length() > limite
                ? texto.substring(0, limite)
                : texto;
    }

    @FunctionalInterface
    interface CarregadorPagina {

        Document carregar(
                String url,
                Map<String, String> formulario
        ) throws Exception;
    }

    private record CidadePublicJud(
            String nome,
            String id
    ) {
    }

    private record EnderecoPublicJud(
            String logradouro,
            String numero,
            String bairro
    ) {
    }
}
