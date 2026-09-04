package br.com.bossolani.judicialpipeline.integration.leiloeiro.djen;

import br.com.bossolani.judicialpipeline.exception.LoteDescartadoException;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.ColetaLeiloeiroDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.LeiloeiroProvider;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.ResilienciaFonteService;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.DadosDinamicosLeilaoDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteDescobertoDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteLeilaoDTO;
import br.com.bossolani.judicialpipeline.model.FonteTipo;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
@Order(60)
public class DjenCnjProvider implements LeiloeiroProvider {

    private static final String NOME = "DJEN/CNJ (fonte oficial)";
    private static final String API_BASE = "https://comunicaapi.pje.jus.br/api/v1/comunicacao";
    private static final String CONSULTA_BASE = "https://comunica.pje.jus.br/consulta";
    private static final String DOMINIO = "comunica.pje.jus.br";

    private static final Pattern CAMINHO_COMUNICACAO = Pattern.compile(
            "^/consulta/([A-Za-z0-9_-]{12,})/?$"
    );

    private static final Pattern PROCESSO_FORMATADO = Pattern.compile(
            "(\\d{7})[-‑](\\d{2})\\.(\\d{4})\\.(\\d)\\.(\\d{2})\\.(\\d{4})"
    );

    private static final Pattern DATA_HORA = Pattern.compile(
            "(\\d{2}/\\d{2}/\\d{4})[^0-9]{0,12}(\\d{1,2}:\\d{2})",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern AVALIACAO = Pattern.compile(
            "(?i)(?:valor\\s+(?:de\\s+)?avalia[cç][aã]o|avaliad[oa](?:\\s+em)?)"
                    + ".{0,120}?R\\$\\s*([\\d.]+,\\d{2})"
    );

    private static final Pattern LANCE_MINIMO_VALOR = Pattern.compile(
            "(?i)(?:lance\\s+m[ií]nimo|pre[cç]o\\s+m[ií]nimo)"
                    + ".{0,100}?R\\$\\s*([\\d.]+,\\d{2})"
    );

    private static final Pattern LANCE_MINIMO_PERCENTUAL = Pattern.compile(
            "(?i)(?:lance\\s+m[ií]nimo|n[aã]o\\s+ser[aã]o\\s+aceitos\\s+lances)"
                    + ".{0,120}?(\\d{1,3})\\s*%"
    );

    private static final Pattern COMISSAO = Pattern.compile(
            "(?i)comiss[aã]o.{0,120}?(\\d+(?:[.,]\\d+)?)\\s*%"
    );

    private static final Pattern VARA = Pattern.compile(
            "(?i)(\\d+[ªa]?\\s+vara(?:\\s+c[ií]vel|\\s+judicial|\\s+da\\s+fazenda)?[^.;]{0,80})"
    );

    private static final Pattern LOGRADOURO = Pattern.compile(
            "(?i)\\b((?:rua|avenida|alameda|rodovia|estrada|pra[cç]a|travessa)"
                    + "\\s+[^.;]{2,180}?)(?=[.;]?\\s*(?:matr[ií]cula|inscri[cç][aã]o|cadastro|avalia[cç][aã]o|$))"
    );

    private static final Pattern ETAPA = Pattern.compile(
            "(?i)(?:^|\\s)([12])\\D{0,4}(?:praca|leilao)\\s*[:.-]?"
    );

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern(
            "dd/MM/yyyy",
            Locale.forLanguageTag("pt-BR")
    );

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final List<String> CIDADES = List.of("Sorocaba", "Votorantim");
    private static final List<String> MARCADORES_IMOVEL = List.of(
            "descricao do imovel",
            "descricao do bem imovel",
            "bem imovel penhorado",
            "imovel objeto do leilao",
            "imovel levado a leilao"
    );

    private final ResilienciaFonteService resiliencia;
    private final int timeoutMs;
    private final int janelaDias;
    private final int maxPaginas;
    private final Clock clock;
    private final ApiClient apiClient;

    @Autowired
    public DjenCnjProvider(
            ResilienciaFonteService resiliencia,
            @Value("${integracao.fontes.timeout-ms:15000}") int timeoutMs,
            @Value("${integracao.djen.janela-dias:45}") int janelaDias,
            @Value("${integracao.djen.max-paginas:2}") int maxPaginas
    ) {
        this(
                resiliencia,
                timeoutMs,
                janelaDias,
                maxPaginas,
                Clock.systemDefaultZone(),
                null
        );
    }

    DjenCnjProvider(
            ResilienciaFonteService resiliencia,
            int timeoutMs,
            int janelaDias,
            int maxPaginas,
            Clock clock,
            ApiClient apiClient
    ) {
        this.resiliencia = resiliencia;
        this.timeoutMs = Math.max(1000, timeoutMs);
        this.janelaDias = Math.max(1, janelaDias);
        this.maxPaginas = Math.max(1, maxPaginas);
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
        this.apiClient = apiClient == null ? this::buscarApiHttp : apiClient;
    }

    @Override
    public String nome() {
        return NOME;
    }

    @Override
    public FonteTipo tipoFonte() {
        return FonteTipo.DJE_TJSP;
    }

    @Override
    public boolean suporta(URI uri) {
        if (uri == null || uri.getHost() == null
                || !"https".equalsIgnoreCase(uri.getScheme())
                || !DOMINIO.equalsIgnoreCase(uri.getHost())) {
            return false;
        }

        return CAMINHO_COMUNICACAO.matcher(uri.getPath()).matches()
                && processoDaQuery(uri) != null;
    }

    @Override
    public String normalizarUrl(URI uri) {
        if (!suporta(uri)) {
            throw new IllegalArgumentException("URL de comunicação do DJEN/CNJ inválida");
        }

        Matcher caminho = CAMINHO_COMUNICACAO.matcher(uri.getPath());
        caminho.matches();
        String processo = processoDaQuery(uri);

        try {
            return new URI(
                    "https",
                    null,
                    DOMINIO,
                    -1,
                    "/consulta/" + caminho.group(1),
                    "numeroProcesso=" + processo,
                    null
            ).toASCIIString();
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("URL de comunicação do DJEN/CNJ inválida", exception);
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
        LocalDate fim = LocalDate.now(clock);
        LocalDate inicio = fim.minusDays(janelaDias);

        for (String cidade : CIDADES) {
            for (int pagina = 1; pagina <= maxPaginas; pagina++) {
                JsonNode resposta = apiClient.buscar(parametrosBusca(
                        cidade + "/SP",
                        inicio,
                        fim,
                        pagina,
                        null
                ));
                JsonNode itens = resposta.path("items");

                if (!itens.isArray()) {
                    break;
                }

                for (JsonNode item : itens) {
                    String texto = limparHtml(item.path("texto").asText(""));
                    String cidadeImovel = cidadeDoImovel(texto);

                    if (!editalElegivel(texto, cidadeImovel)
                            || !cidade.equals(cidadeImovel)) {
                        continue;
                    }

                    String processo = processoDoItem(item, texto);
                    String hash = item.path("hash").asText("").trim();
                    if (processo == null || hash.isBlank()) {
                        continue;
                    }

                    String url = urlComunicacao(hash, processo);
                    lotes.putIfAbsent(
                            url,
                            new LoteDescobertoDTO(
                                    url,
                                    inferirTipo(texto) + " em " + cidadeImovel,
                                    cidadeImovel,
                                    limitarTexto(texto, 4000),
                                    NOME
                            )
                    );
                }

                if (itens.size() < 100) {
                    break;
                }
            }
        }

        return new ArrayList<>(lotes.values());
    }

    private ColetaLeiloeiroDTO coletarSemRetentativa(String url) throws Exception {
        URI uri = URI.create(url);
        String urlNormalizada = normalizarUrl(uri);
        String processoConsulta = processoDaQuery(uri);
        String hash = hashDoCaminho(uri);
        LocalDate fim = LocalDate.now(clock);
        JsonNode resposta = apiClient.buscar(parametrosBusca(
                null,
                fim.minusDays(Math.max(180, janelaDias)),
                fim,
                1,
                processoConsulta
        ));
        JsonNode item = localizarItem(resposta.path("items"), hash, processoConsulta);

        if (item == null) {
            throw new IllegalArgumentException("Comunicação do DJEN/CNJ não foi localizada novamente");
        }

        String texto = limparHtml(item.path("texto").asText(""));
        String cidade = cidadeDoImovel(texto);
        if (!editalElegivel(texto, cidade)) {
            throw new LoteDescartadoException(
                    "Comunicação do DJEN/CNJ não é edital unitário de leilão de imóvel na região."
            );
        }

        String processo = processoDoItem(item, texto);
        BigDecimal avaliacao = extrairValor(texto, AVALIACAO);
        BigDecimal lanceMinimo = extrairValor(texto, LANCE_MINIMO_VALOR);
        Integer percentualMinimo = extrairPercentual(texto);

        if (lanceMinimo == null && avaliacao != null && percentualMinimo != null) {
            lanceMinimo = avaliacao
                    .multiply(BigDecimal.valueOf(percentualMinimo))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        Map<Integer, PracaDjen> pracas = extrairPracas(texto);
        PracaDjen primeira = pracas.get(1);
        PracaDjen segunda = pracas.get(2);
        EnderecoDjen endereco = extrairEndereco(texto, cidade);

        LoteLeilaoDTO lote = new LoteLeilaoDTO(
                processo,
                avaliacao,
                cidade,
                extrairVara(item, texto),
                inferirTipo(texto),
                endereco.logradouro(),
                endereco.numero(),
                endereco.bairro(),
                urlNormalizada
        );

        DadosDinamicosLeilaoDTO leilao = new DadosDinamicosLeilaoDTO(
                abertura(primeira),
                fechamento(primeira),
                avaliacao,
                abertura(segunda),
                fechamento(segunda),
                lanceMinimo,
                calcularDesconto(avaliacao, lanceMinimo, percentualMinimo),
                definirStatus(texto, primeira, segunda),
                definirResultado(texto),
                lanceMinimo == null ? avaliacao : lanceMinimo,
                null,
                extrairDecimal(texto, COMISSAO)
        );

        return new ColetaLeiloeiroDTO(lote, leilao);
    }

    private boolean editalElegivel(String texto, String cidade) {
        if (texto == null || texto.isBlank() || cidade == null) {
            return false;
        }

        String normalizado = normalizar(texto);
        int edital = indicePrimeiro(normalizado, List.of(
                "edital de leilao judicial",
                "edital de leilao eletronico",
                "edital de praca e leilao"
        ));
        int descricao = indicePrimeiro(normalizado, MARCADORES_IMOVEL);

        if (edital < 0 || edital > 700 || descricao < 0) {
            return false;
        }

        int fimTrecho = Math.min(normalizado.length(), descricao + 5000);
        String trechoImovel = normalizado.substring(descricao, fimTrecho);

        return trechoImovel.contains(normalizar(cidade))
                && contemIndicadorImovel(trechoImovel)
                && extrairProcesso(texto) != null;
    }

    private String cidadeDoImovel(String texto) {
        String normalizado = normalizar(texto);
        int descricao = indicePrimeiro(normalizado, MARCADORES_IMOVEL);
        if (descricao < 0) {
            return null;
        }

        String trecho = normalizado.substring(
                descricao,
                Math.min(normalizado.length(), descricao + 5000)
        );
        boolean sorocaba = cidadeNoTrecho(trecho, "sorocaba");
        boolean votorantim = cidadeNoTrecho(trecho, "votorantim");

        if (sorocaba == votorantim) {
            return null;
        }
        return votorantim ? "Votorantim" : "Sorocaba";
    }

    private boolean cidadeNoTrecho(String trecho, String cidade) {
        return Pattern.compile("(^|[^a-z])" + cidade + "(?:\\s*[/,-]\\s*sp)?([^a-z]|$)")
                .matcher(trecho)
                .find();
    }

    private boolean contemIndicadorImovel(String texto) {
        return List.of(
                "imovel", "apartamento", "casa", "sobrado", "terreno",
                "gleba", "galpao", "predio", "sala comercial"
        ).stream().anyMatch(texto::contains);
    }

    private Map<Integer, PracaDjen> extrairPracas(String texto) {
        Map<Integer, PracaDjen> pracas = new LinkedHashMap<>();
        String textoNormalizado = normalizar(texto);
        Matcher matcher = ETAPA.matcher(textoNormalizado);
        List<InicioEtapa> inicios = new ArrayList<>();

        while (matcher.find()) {
            inicios.add(new InicioEtapa(Integer.parseInt(matcher.group(1)), matcher.start()));
        }

        for (int indice = 0; indice < inicios.size(); indice++) {
            InicioEtapa atual = inicios.get(indice);
            int fim = indice + 1 < inicios.size()
                    ? inicios.get(indice + 1).inicio()
                    : Math.min(textoNormalizado.length(), atual.inicio() + 1800);
            String trecho = textoNormalizado.substring(atual.inicio(), fim);
            Matcher dataMatcher = DATA_HORA.matcher(trecho);
            List<LocalDateTime> datas = new ArrayList<>();

            while (dataMatcher.find() && datas.size() < 2) {
                LocalDateTime data = parseData(dataMatcher.group(1), dataMatcher.group(2));
                if (data != null) {
                    datas.add(data);
                }
            }

            if (!datas.isEmpty()) {
                pracas.putIfAbsent(
                        atual.ordem(),
                        new PracaDjen(datas.getFirst(), datas.getLast())
                );
            }
        }

        return pracas;
    }

    private EnderecoDjen extrairEndereco(String texto, String cidade) {
        String normalizado = normalizar(texto);
        int descricao = indicePrimeiro(normalizado, MARCADORES_IMOVEL);
        String trecho = descricao < 0
                ? texto
                : texto.substring(descricao, Math.min(texto.length(), descricao + 5000));
        Matcher matcher = LOGRADOURO.matcher(trecho);

        if (!matcher.find()) {
            return new EnderecoDjen("Endereço descrito no edital oficial", null, null);
        }

        String completo = matcher.group(1)
                .replaceAll("(?i)\\s*,?\\s*" + Pattern.quote(cidade) + "\\s*[-/]?\\s*SP.*$", "")
                .trim();
        String[] partes = completo.split("\\s*,\\s*");
        String logradouro = partes[0].trim();
        String numero = partes.length > 1 ? extrairNumero(partes[1]) : null;
        String bairro = null;

        for (int i = 2; i < partes.length; i++) {
            String parte = partes[i].trim();
            if (!normalizar(parte).contains(normalizar(cidade))
                    && !parte.matches("(?i)SP|CEP.*")) {
                bairro = parte;
            }
        }

        return new EnderecoDjen(logradouro, valorOuNulo(numero), valorOuNulo(bairro));
    }

    private String extrairNumero(String trecho) {
        Matcher matcher = Pattern.compile("(?i)(?:n[º°o.]?\\s*)?(\\d+[A-Za-z]?)").matcher(trecho);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return trecho.matches("(?i)s/?n") ? "s/n" : null;
    }

    private String extrairVara(JsonNode item, String texto) {
        String orgao = item.path("nomeOrgao").asText("").trim();
        if (!orgao.isBlank()) {
            return orgao;
        }
        Matcher matcher = VARA.matcher(texto);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private String inferirTipo(String texto) {
        String normalizado = normalizar(texto);
        int descricao = indicePrimeiro(normalizado, MARCADORES_IMOVEL);
        String trecho = descricao < 0
                ? normalizado
                : normalizado.substring(descricao, Math.min(normalizado.length(), descricao + 1200));
        if (trecho.contains("apartamento")) return "Apartamento";
        if (trecho.contains("galpao") || trecho.contains("industrial")) return "Galpão";
        if (trecho.contains("terreno") || trecho.contains("gleba")) return "Terreno";
        if (trecho.contains("sobrado")) return "Sobrado";
        if (trecho.contains("casa")) return "Casa";
        if (trecho.contains("predio")) return "Prédio";
        if (trecho.contains("sala comercial")) return "Sala";
        return "Imóvel";
    }

    private String definirStatus(String texto, PracaDjen primeira, PracaDjen segunda) {
        String normalizado = normalizar(texto);
        if (normalizado.contains("leilao cancelado") || normalizado.contains("praca cancelada")) {
            return "CANCELADO";
        }
        if (normalizado.contains("leilao suspenso") || normalizado.contains("praca suspensa")) {
            return "SUSPENSO";
        }

        LocalDateTime agora = LocalDateTime.now(clock);
        LocalDateTime fim = segunda != null ? segunda.fechamento() : fechamento(primeira);
        if (fim != null && agora.isAfter(fim)) {
            return "ENCERRADO";
        }
        LocalDateTime inicio = primeira == null ? null : primeira.abertura();
        if (inicio != null && agora.isBefore(inicio)) {
            return "AGENDADO";
        }
        return fim == null ? "DESCONHECIDO" : "EM_ANDAMENTO";
    }

    private String definirResultado(String texto) {
        String normalizado = normalizar(texto);
        if (normalizado.contains("sem licitante") || normalizado.contains("sem lances")) {
            return "SEM LANCES";
        }
        if (normalizado.contains("arrematado") || normalizado.contains("vendido")) {
            return "ARREMATADO";
        }
        return "DESCONHECIDO";
    }

    private JsonNode localizarItem(JsonNode itens, String hash, String processo) {
        if (!itens.isArray()) {
            return null;
        }
        for (JsonNode item : itens) {
            if (hash.equals(item.path("hash").asText())
                    || processo.equals(digitos(item.path("numero_processo").asText()))) {
                return item;
            }
        }
        return null;
    }

    private Map<String, String> parametrosBusca(
            String texto,
            LocalDate inicio,
            LocalDate fim,
            int pagina,
            String processo
    ) {
        Map<String, String> parametros = new LinkedHashMap<>();
        parametros.put("siglaTribunal", "TJSP");
        parametros.put("meio", "E");
        parametros.put("dataDisponibilizacaoInicio", inicio.toString());
        parametros.put("dataDisponibilizacaoFim", fim.toString());
        parametros.put("itensPorPagina", "100");
        parametros.put("pagina", Integer.toString(pagina));
        if (texto != null) parametros.put("texto", texto);
        if (processo != null) parametros.put("numeroProcesso", processo);
        return parametros;
    }

    private JsonNode buscarApiHttp(Map<String, String> parametros) throws Exception {
        String query = parametros.entrySet().stream()
                .map(item -> codificar(item.getKey()) + "=" + codificar(item.getValue()))
                .reduce((a, b) -> a + "&" + b)
                .orElse("");
        String corpo = Jsoup.connect(API_BASE + "?" + query)
                .userAgent("JudicialPipeline/1.3 (+consulta-publica-djen)")
                .header("Accept", "application/json")
                .ignoreContentType(true)
                .timeout(timeoutMs)
                .maxBodySize(0)
                .execute()
                .body();
        return OBJECT_MAPPER.readTree(corpo);
    }

    private String limparHtml(String html) {
        Document documento = Jsoup.parse(html == null ? "" : html);
        documento.select("style, script, noscript").remove();
        return documento.text()
                .replace('‑', '-')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String processoDoItem(JsonNode item, String texto) {
        String processo = digitos(item.path("numero_processo").asText(""));
        if (processo.length() == 20) {
            return formatarProcesso(processo);
        }
        return extrairProcesso(texto);
    }

    private String extrairProcesso(String texto) {
        Matcher matcher = PROCESSO_FORMATADO.matcher(texto == null ? "" : texto);
        return matcher.find()
                ? String.join("", matcher.group(1), "-", matcher.group(2), ".",
                matcher.group(3), ".", matcher.group(4), ".", matcher.group(5), ".", matcher.group(6))
                : null;
    }

    private String formatarProcesso(String processo) {
        return processo.substring(0, 7) + "-" + processo.substring(7, 9)
                + "." + processo.substring(9, 13)
                + "." + processo.substring(13, 14)
                + "." + processo.substring(14, 16)
                + "." + processo.substring(16, 20);
    }

    private String urlComunicacao(String hash, String processo) {
        return CONSULTA_BASE + "/" + hash + "?numeroProcesso=" + digitos(processo);
    }

    private String processoDaQuery(URI uri) {
        if (uri == null || uri.getRawQuery() == null) {
            return null;
        }
        for (String parametro : uri.getRawQuery().split("&")) {
            String[] partes = parametro.split("=", 2);
            if (partes.length == 2 && "numeroProcesso".equalsIgnoreCase(partes[0])) {
                String processo = digitos(partes[1]);
                return processo.length() == 20 ? processo : null;
            }
        }
        return null;
    }

    private String hashDoCaminho(URI uri) {
        Matcher matcher = CAMINHO_COMUNICACAO.matcher(uri.getPath());
        return matcher.matches() ? matcher.group(1) : "";
    }

    private int indicePrimeiro(String texto, List<String> termos) {
        int indice = -1;
        for (String termo : termos) {
            int atual = texto.indexOf(termo);
            if (atual >= 0 && (indice < 0 || atual < indice)) {
                indice = atual;
            }
        }
        return indice;
    }

    private BigDecimal extrairValor(String texto, Pattern pattern) {
        Matcher matcher = pattern.matcher(texto == null ? "" : texto);
        return matcher.find() ? monetario(matcher.group(1)) : null;
    }

    private BigDecimal extrairDecimal(String texto, Pattern pattern) {
        Matcher matcher = pattern.matcher(texto == null ? "" : texto);
        return matcher.find() ? new BigDecimal(matcher.group(1).replace(',', '.')) : null;
    }

    private Integer extrairPercentual(String texto) {
        Matcher matcher = LANCE_MINIMO_PERCENTUAL.matcher(texto);
        if (!matcher.find()) return null;
        int percentual = Integer.parseInt(matcher.group(1));
        return percentual > 0 && percentual <= 100 ? percentual : null;
    }

    private Integer calcularDesconto(BigDecimal avaliacao, BigDecimal lance, Integer percentual) {
        if (percentual != null) return Math.max(0, 100 - percentual);
        if (avaliacao == null || lance == null || avaliacao.signum() <= 0) return null;
        int percentualLance = lance.multiply(BigDecimal.valueOf(100))
                .divide(avaliacao, 0, RoundingMode.HALF_UP).intValue();
        return Math.max(0, 100 - percentualLance);
    }

    private BigDecimal monetario(String valor) {
        return valor == null ? null : new BigDecimal(valor.replace(".", "").replace(',', '.'));
    }

    private LocalDateTime parseData(String data, String hora) {
        try {
            return LocalDateTime.of(
                    LocalDate.parse(data, FORMATO_DATA),
                    LocalTime.parse(hora.length() == 4 ? "0" + hora : hora)
            );
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private String normalizar(String texto) {
        return Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private String digitos(String texto) {
        return texto == null ? "" : texto.replaceAll("\\D", "");
    }

    private String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }

    private String limitarTexto(String texto, int limite) {
        return texto.length() <= limite ? texto : texto.substring(0, limite);
    }

    private String valorOuNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private LocalDateTime abertura(PracaDjen praca) {
        return praca == null ? null : praca.abertura();
    }

    private LocalDateTime fechamento(PracaDjen praca) {
        return praca == null ? null : praca.fechamento();
    }

    @FunctionalInterface
    interface ApiClient {
        JsonNode buscar(Map<String, String> parametros) throws Exception;
    }

    private record InicioEtapa(int ordem, int inicio) {
    }

    private record PracaDjen(LocalDateTime abertura, LocalDateTime fechamento) {
    }

    private record EnderecoDjen(String logradouro, String numero, String bairro) {
    }
}
