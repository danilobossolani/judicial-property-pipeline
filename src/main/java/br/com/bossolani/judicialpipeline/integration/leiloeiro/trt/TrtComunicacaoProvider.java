package br.com.bossolani.judicialpipeline.integration.leiloeiro.trt;

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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Coleta editais publicados na Plataforma Nacional de Editais do CNJ para um
 * Tribunal Regional do Trabalho específico. O parser aceita tanto editais
 * unitários quanto pautas unificadas com diversos processos e lotes.
 */
public abstract class TrtComunicacaoProvider implements LeiloeiroProvider {

    private static final String API_BASE =
            "https://comunicaapi.pje.jus.br/api/v1/comunicacao";
    private static final String CONSULTA_BASE =
            "https://comunica.pje.jus.br/consulta";
    private static final String DOMINIO = "comunica.pje.jus.br";
    private static final String TERMO_BUSCA = "leilão";
    private static final int ITENS_POR_PAGINA = 100;

    private static final Pattern CAMINHO_COMUNICACAO = Pattern.compile(
            "^/consulta/([A-Za-z0-9_-]{12,})/?$"
    );
    private static final Pattern PROCESSO_FORMATADO = Pattern.compile(
            "(\\d{7})[-‑](\\d{2})\\.(\\d{4})\\.(\\d)\\.(\\d{2})\\.(\\d{4})"
    );
    private static final Pattern CABECALHO_PROCESSO = Pattern.compile(
            "(?iu)(?:^|\\s)(\\d{1,3})\\s*:\\s*"
                    + "((?:\\d{7})[-‑](?:\\d{2})\\.(?:\\d{4})\\.(?:\\d)\\.(?:\\d{2})\\.(?:\\d{4}))"
                    + "\\s*-"
    );
    private static final Pattern SUBLOTE = Pattern.compile(
            "(?iu)(\\d{1,3}\\.\\d{1,3})\\s+Tipo\\s+do\\s+Bem\\s*:\\s*"
                    + "(.+?)(?=\\s+Identifica[cç][aã]o\\s*:|\\s+Descri[cç][aã]o\\s*:|$)"
    );
    private static final Pattern EDITAL_LEILAO = Pattern.compile(
            "(?iu)edital\\s+de\\s+leil[aã]o(?:\\s+judicial|\\s+da\\s+hasta\\s+p[uú]blica)"
    );
    private static final Pattern IMOVEL = Pattern.compile(
            "(?iu)(?:Tipo\\s+do\\s+Bem\\s*:\\s*Im[oó]vel|"
                    + "(?:nua\\s+propriedade\\s+do\\s+)?im[oó]vel\\s+de\\s+matr[ií]cula|"
                    + "descri[cç][aã]o\\s+do\\s+(?:bem\\s+)?im[oó]vel|"
                    + "bem\\s+im[oó]vel\\s+penhorado)"
    );
    private static final Pattern DATA_NUMERICA = Pattern.compile(
            "(?iu)(?:at[eé]\\s+o\\s+)?(?:no\\s+)?dia\\s+"
                    + "(\\d{1,2})/(\\d{1,2})/(\\d{4})\\s*,?\\s*"
                    + "(?:[aà]s?)\\s+(\\d{1,2})[:h](\\d{2})"
    );
    private static final Pattern DATA_EXTENSO = Pattern.compile(
            "(?iu)(?:at[eé]\\s+o\\s+)?(?:no\\s+)?dia\\s+"
                    + "(\\d{1,2})\\s+de\\s+([A-Za-zçÇ]+)\\s+de\\s+(\\d{4})"
                    + "\\s*,?\\s*(?:[aà]s?)\\s+(\\d{1,2})H(\\d{2})"
    );
    private static final Pattern INTERVALO_ESCALONADO = Pattern.compile(
            "(?iu)(?:a\\s+cada|intervalo\\s+de)\\s+(\\d{1,2})\\s+minutos"
    );
    private static final Pattern VALOR_AVALIACAO = Pattern.compile(
            "(?iu)(?:Total\\s+da\\s+avalia[cç][aã]o|Valor\\s+Total\\s+Penhorado|"
                    + "Valor\\s+(?:de\\s+)?Avalia[cç][aã]o|Avalia[cç][aã]o)"
                    + "\\s*:?\\s*R\\$\\s*([\\d.]+,\\d{2})"
    );
    private static final Pattern VALOR_LANCE_MINIMO = Pattern.compile(
            "(?iu)(?:Valor\\s+)?Lance\\s+M[ií]nimo(?:\\s+do\\s+leil[aã]o)?"
                    + "(?:\\s*\\([^)]*\\))?\\s*:?\\s*R\\$\\s*([\\d.]+,\\d{2})"
    );
    private static final Pattern PERCENTUAL_LANCE = Pattern.compile(
            "(?iu)(?:Valor\\s+)?Lance\\s+M[ií]nimo\\s*\\((\\d{1,3})%\\)"
    );
    private static final Pattern COMISSAO = Pattern.compile(
            "(?iu)Comiss[aã]o\\s+do\\(a\\)\\s+Leiloeiro\\(a\\)|"
                    + "(?iu:Comiss[aã]o\\s+do\\s+Leiloeiro)"
    );
    private static final Pattern PERCENTUAL = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?)\\s*%"
    );
    private static final Pattern LOCAL_DOS_BENS = Pattern.compile(
            "(?iu)Local\\s+dos\\s+bens\\s*:\\s*(.+?)"
                    + "(?=\\s+(?:Total\\s+da\\s+avalia[cç][aã]o|"
                    + "Avalia[cç][aã]o|Lance\\s+m[ií]nimo|Leiloeiro\\(a\\)|$))"
    );
    private static final Pattern LOCALIZACAO = Pattern.compile(
            "(?iu)Localiza[cç][aã]o\\s*:\\s*(.+?)"
                    + "(?=\\s+(?:Quantidade|Percentual\\s+da\\s+Penhora|"
                    + "Valor\\s+Unit[aá]rio|Valor\\s+Total|Data\\s+da\\s+Avalia[cç][aã]o|$))"
    );
    private static final Pattern NUMERO_ROTULADO = Pattern.compile(
            "(?iu)N[uú]mero\\s*:\\s*([^,]+)"
    );
    private static final Pattern BAIRRO_ROTULADO = Pattern.compile(
            "(?iu)Bairro\\s*:\\s*(.+?)(?=,?\\s*Cidade\\s*:|$)"
    );
    private static final Pattern CIDADE_ROTULADA = Pattern.compile(
            "(?iu)Cidade\\s*:\\s*(Sorocaba|Votorantim)\\s*,?\\s*UF\\s*:\\s*SP"
    );
    private static final Pattern CIDADE_UF = Pattern.compile(
            "(?iu)(Sorocaba|Votorantim)\\s*/\\s*SP"
    );

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String nome;
    private final String siglaTribunal;
    private final String codigoProcesso;
    private final ResilienciaFonteService resiliencia;
    private final int timeoutMs;
    private final int janelaDias;
    private final int maxPaginas;
    private final Clock clock;
    private final ApiClient apiClient;

    protected TrtComunicacaoProvider(
            String nome,
            String siglaTribunal,
            String codigoProcesso,
            ResilienciaFonteService resiliencia,
            int timeoutMs,
            int janelaDias,
            int maxPaginas
    ) {
        this(
                nome,
                siglaTribunal,
                codigoProcesso,
                resiliencia,
                timeoutMs,
                janelaDias,
                maxPaginas,
                Clock.systemDefaultZone(),
                null
        );
    }

    protected TrtComunicacaoProvider(
            String nome,
            String siglaTribunal,
            String codigoProcesso,
            ResilienciaFonteService resiliencia,
            int timeoutMs,
            int janelaDias,
            int maxPaginas,
            Clock clock,
            ApiClient apiClient
    ) {
        this.nome = nome;
        this.siglaTribunal = siglaTribunal;
        this.codigoProcesso = codigoProcesso;
        this.resiliencia = resiliencia;
        this.timeoutMs = Math.max(1000, timeoutMs);
        this.janelaDias = Math.max(1, janelaDias);
        this.maxPaginas = Math.max(1, maxPaginas);
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
        this.apiClient = apiClient == null ? this::buscarApiHttp : apiClient;
    }

    @Override
    public String nome() {
        return nome;
    }

    @Override
    public FonteTipo tipoFonte() {
        // Mantém compatibilidade com os registros de diários oficiais já existentes.
        return FonteTipo.DJE_TJSP;
    }

    @Override
    public boolean suporta(URI uri) {
        if (uri == null
                || uri.getHost() == null
                || !"https".equalsIgnoreCase(uri.getScheme())
                || !DOMINIO.equalsIgnoreCase(uri.getHost())
                || !CAMINHO_COMUNICACAO.matcher(uri.getPath()).matches()) {
            return false;
        }

        String processoPai = parametro(uri, "numeroProcesso");
        String processoLote = parametro(uri, "processoLote");
        String processo = digitos(
                processoLote == null ? processoPai : processoLote
        );

        return processo.length() == 20
                && codigoProcesso.equals(processo.substring(13, 16));
    }

    @Override
    public String normalizarUrl(URI uri) {
        if (!suporta(uri)) {
            throw new IllegalArgumentException(
                    "URL de comunicação do " + siglaTribunal + " inválida"
            );
        }

        Matcher caminho = CAMINHO_COMUNICACAO.matcher(uri.getPath());
        caminho.matches();
        String processoPai = digitos(parametro(uri, "numeroProcesso"));
        String processoLote = digitos(parametro(uri, "processoLote"));
        if (processoLote.isBlank()) {
            processoLote = processoPai;
        }
        String lote = valorOuNulo(parametro(uri, "lote"));

        String query = "numeroProcesso=" + processoPai
                + "&processoLote=" + processoLote
                + (lote == null ? "" : "&lote=" + codificar(lote));

        try {
            return new URI(
                    "https",
                    null,
                    DOMINIO,
                    -1,
                    "/consulta/" + caminho.group(1),
                    query,
                    null
            ).toASCIIString();
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException(
                    "URL de comunicação do " + siglaTribunal + " inválida",
                    exception
            );
        }
    }

    @Override
    public List<LoteDescobertoDTO> descobrirLotes() throws Exception {
        return resiliencia.executar(nome, this::descobrirSemRetentativa);
    }

    @Override
    public ColetaLeiloeiroDTO coletar(String url) throws Exception {
        return resiliencia.executar(nome, () -> coletarSemRetentativa(url));
    }

    private List<LoteDescobertoDTO> descobrirSemRetentativa() throws Exception {
        LocalDate fim = LocalDate.now(clock);
        LocalDate inicio = fim.minusDays(janelaDias);
        Map<String, LoteDescobertoDTO> lotes = new LinkedHashMap<>();
        Set<String> editaisAnalisados = new LinkedHashSet<>();

        for (int pagina = 1; pagina <= maxPaginas; pagina++) {
            JsonNode resposta = apiClient.buscar(parametrosBusca(
                    inicio,
                    fim,
                    pagina,
                    TERMO_BUSCA,
                    null
            ));
            JsonNode itens = resposta.path("items");

            if (!itens.isArray() || itens.isEmpty()) {
                break;
            }

            for (JsonNode item : itens) {
                String texto = limparHtml(item.path("texto").asText(""));
                String assinatura = texto.length() + ":" + Integer.toHexString(texto.hashCode());
                if (!editaisAnalisados.add(assinatura)) {
                    continue;
                }

                for (LoteTrabalhista lote : extrairLotes(item, texto)) {
                    String chave = digitos(lote.processo()) + "#" + lote.identificador();
                    lotes.putIfAbsent(
                            chave,
                            new LoteDescobertoDTO(
                                    lote.url(),
                                    lote.tipo() + " em " + lote.cidade(),
                                    lote.cidade(),
                                    "Imóvel em " + lote.cidade()
                                            + " publicado pelo " + siglaTribunal
                                            + " no processo " + lote.processo() + ".",
                                    nome
                            )
                    );
                }
            }

            int total = resposta.path("count").asInt(0);
            if (itens.size() < ITENS_POR_PAGINA
                    || pagina * ITENS_POR_PAGINA >= total) {
                break;
            }
        }

        return new ArrayList<>(lotes.values());
    }

    private ColetaLeiloeiroDTO coletarSemRetentativa(String url) throws Exception {
        URI uri = URI.create(url);
        String urlNormalizada = normalizarUrl(uri);
        String processoPai = digitos(parametro(uri, "numeroProcesso"));
        String processoLote = digitos(parametro(uri, "processoLote"));
        if (processoLote.isBlank()) {
            processoLote = processoPai;
        }
        final String processoLoteSelecionado = processoLote;
        String identificador = valorOuNulo(parametro(uri, "lote"));
        String hash = hashDoCaminho(uri);
        JsonNode item = buscarComunicacao(hash, processoPai);

        if (item == null) {
            throw new IllegalArgumentException(
                    "Comunicação oficial do " + siglaTribunal + " não foi localizada novamente"
            );
        }

        String texto = limparHtml(item.path("texto").asText(""));
        LoteTrabalhista selecionado = extrairLotes(item, texto).stream()
                .filter(lote -> digitos(lote.processo()).equals(processoLoteSelecionado))
                .filter(lote -> identificador == null
                        || identificador.equalsIgnoreCase(lote.identificador()))
                .findFirst()
                .orElseThrow(() -> new LoteDescartadoException(
                        "O edital do " + siglaTribunal
                                + " não contém mais o imóvel identificado na região."
                ));

        BigDecimal avaliacao = extrairValor(
                selecionado.conteudo(),
                VALOR_AVALIACAO
        );
        BigDecimal lanceMinimo = extrairValor(
                selecionado.conteudo(),
                VALOR_LANCE_MINIMO
        );
        Integer percentualLance = extrairInteiro(
                selecionado.conteudo(),
                PERCENTUAL_LANCE
        );
        if (lanceMinimo == null && avaliacao != null && percentualLance != null) {
            lanceMinimo = avaliacao
                    .multiply(BigDecimal.valueOf(percentualLance))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        EnderecoTrabalhista endereco = extrairEndereco(
                selecionado.conteudo(),
                selecionado.cidade()
        );
        LocalDateTime encerramento = encerramentoDoLote(
                texto,
                selecionado.identificador()
        );
        BigDecimal comissao = extrairComissao(texto);

        LoteLeilaoDTO lote = new LoteLeilaoDTO(
                selecionado.processo(),
                avaliacao,
                selecionado.cidade(),
                selecionado.orgao(),
                selecionado.tipo(),
                endereco.logradouro(),
                endereco.numero(),
                endereco.bairro(),
                urlNormalizada
        );

        DadosDinamicosLeilaoDTO leilao = new DadosDinamicosLeilaoDTO(
                encerramento,
                encerramento,
                lanceMinimo == null ? avaliacao : lanceMinimo,
                null,
                null,
                null,
                calcularDesconto(avaliacao, lanceMinimo, percentualLance),
                definirStatus(texto, item.path("ativo").asBoolean(true), encerramento),
                definirResultado(texto),
                lanceMinimo == null ? avaliacao : lanceMinimo,
                null,
                comissao
        );

        return new ColetaLeiloeiroDTO(lote, leilao);
    }

    private JsonNode buscarComunicacao(String hash, String processoPai) throws Exception {
        LocalDate fim = LocalDate.now(clock);
        LocalDate inicio = fim.minusYears(5);

        for (int pagina = 1; pagina <= maxPaginas; pagina++) {
            JsonNode resposta = apiClient.buscar(parametrosBusca(
                    inicio,
                    fim,
                    pagina,
                    null,
                    processoPai
            ));
            JsonNode itens = resposta.path("items");
            if (!itens.isArray()) {
                return null;
            }
            for (JsonNode item : itens) {
                if (hash.equals(item.path("hash").asText(""))) {
                    return item;
                }
            }
            int total = resposta.path("count").asInt(0);
            if (itens.size() < ITENS_POR_PAGINA
                    || pagina * ITENS_POR_PAGINA >= total) {
                break;
            }
        }
        return null;
    }

    private List<LoteTrabalhista> extrairLotes(JsonNode item, String texto) {
        if (texto == null
                || texto.isBlank()
                || !EDITAL_LEILAO.matcher(texto).find()) {
            return List.of();
        }

        String hash = item.path("hash").asText("").trim();
        String processoPai = processoDoItem(item);
        if (hash.isBlank() || processoPai == null) {
            return List.of();
        }

        List<BlocoProcesso> blocos = separarBlocos(texto, processoPai);
        List<LoteTrabalhista> lotes = new ArrayList<>();

        for (BlocoProcesso bloco : blocos) {
            for (BlocoBem bem : separarBens(bloco)) {
                if (!IMOVEL.matcher(bem.conteudo()).find()) {
                    continue;
                }

                String cidade = cidadeDoImovel(bem.conteudo());
                if (cidade == null) {
                    continue;
                }

                String tipo = inferirTipo(bem.conteudo());
                String url = urlComunicacao(
                        hash,
                        processoPai,
                        bloco.processo(),
                        bem.identificador()
                );
                lotes.add(new LoteTrabalhista(
                        bloco.processo(),
                        bem.identificador(),
                        cidade,
                        tipo,
                        bem.conteudo(),
                        valorOuPadrao(item.path("nomeOrgao").asText(""), siglaTribunal),
                        url
                ));
            }
        }

        return lotes;
    }

    private List<BlocoProcesso> separarBlocos(String texto, String processoPai) {
        Matcher matcher = CABECALHO_PROCESSO.matcher(texto);
        List<InicioProcesso> inicios = new ArrayList<>();
        while (matcher.find()) {
            inicios.add(new InicioProcesso(
                    matcher.start(),
                    matcher.group(1),
                    matcher.group(2)
            ));
        }

        if (inicios.isEmpty()) {
            return List.of(new BlocoProcesso(
                    processoPai,
                    "unico",
                    texto
            ));
        }

        List<BlocoProcesso> blocos = new ArrayList<>();
        for (int indice = 0; indice < inicios.size(); indice++) {
            InicioProcesso atual = inicios.get(indice);
            int fim = indice + 1 < inicios.size()
                    ? inicios.get(indice + 1).inicio()
                    : texto.length();
            blocos.add(new BlocoProcesso(
                    atual.processo(),
                    atual.identificador(),
                    texto.substring(atual.inicio(), fim)
            ));
        }
        return blocos;
    }

    private List<BlocoBem> separarBens(BlocoProcesso bloco) {
        Matcher matcher = SUBLOTE.matcher(bloco.conteudo());
        List<InicioBem> inicios = new ArrayList<>();
        while (matcher.find()) {
            inicios.add(new InicioBem(
                    matcher.start(),
                    matcher.group(1),
                    matcher.group(2)
            ));
        }

        if (inicios.isEmpty()) {
            Matcher imovel = IMOVEL.matcher(bloco.conteudo());
            String conteudo = imovel.find()
                    ? bloco.conteudo().substring(imovel.start())
                    : bloco.conteudo();
            return List.of(new BlocoBem(
                    bloco.identificador(),
                    conteudo
            ));
        }

        List<BlocoBem> bens = new ArrayList<>();
        for (int indice = 0; indice < inicios.size(); indice++) {
            InicioBem atual = inicios.get(indice);
            int fim = indice + 1 < inicios.size()
                    ? inicios.get(indice + 1).inicio()
                    : bloco.conteudo().length();
            if (normalizar(atual.tipo()).contains("imovel")) {
                bens.add(new BlocoBem(
                        atual.identificador(),
                        bloco.conteudo().substring(atual.inicio(), fim)
                ));
            }
        }
        return bens;
    }

    private String cidadeDoImovel(String conteudo) {
        Matcher rotulada = CIDADE_ROTULADA.matcher(conteudo);
        if (rotulada.find()) {
            return capitalizarCidade(rotulada.group(1));
        }

        Matcher cidadeUf = CIDADE_UF.matcher(conteudo);
        Set<String> cidades = new LinkedHashSet<>();
        while (cidadeUf.find()) {
            cidades.add(capitalizarCidade(cidadeUf.group(1)));
        }
        return cidades.size() == 1 ? cidades.iterator().next() : null;
    }

    private String inferirTipo(String conteudo) {
        String normalizado = normalizar(conteudo);
        if (normalizado.contains("apartamento")) return "Apartamento";
        if (normalizado.contains("galpao") || normalizado.contains("industrial")) return "Galpão";
        if (normalizado.contains("sobrado")) return "Sobrado";
        if (normalizado.contains("casa")) return "Casa";
        if (normalizado.contains("predio")) return "Prédio";
        if (normalizado.contains("sala comercial")) return "Sala comercial";
        if (normalizado.contains("terreno") || normalizado.contains("gleba")) return "Terreno";
        return "Imóvel";
    }

    private EnderecoTrabalhista extrairEndereco(String conteudo, String cidade) {
        Matcher localizacao = LOCALIZACAO.matcher(conteudo);
        if (localizacao.find()) {
            String completo = localizacao.group(1).trim();
            String numero = grupoOuNulo(NUMERO_ROTULADO, completo, 1);
            String bairro = grupoOuNulo(BAIRRO_ROTULADO, completo, 1);
            String logradouro = completo
                    .replaceFirst("(?iu)\\s*N[uú]mero\\s*:.*$", "")
                    .trim();
            return new EnderecoTrabalhista(
                    valorOuPadrao(logradouro, "Endereço descrito no edital oficial"),
                    numero,
                    bairro
            );
        }

        Matcher localDosBens = LOCAL_DOS_BENS.matcher(conteudo);
        if (localDosBens.find()) {
            return separarEnderecoLivre(localDosBens.group(1), cidade);
        }

        return new EnderecoTrabalhista(
                "Endereço descrito no edital oficial",
                null,
                null
        );
    }

    private EnderecoTrabalhista separarEnderecoLivre(String endereco, String cidade) {
        String semCidade = endereco
                .replaceFirst(
                        "(?iu),?\\s*" + Pattern.quote(cidade)
                                + "\\s*/\\s*SP.*$",
                        ""
                )
                .trim();
        String[] partes = semCidade.split("\\s*,\\s*");
        String logradouro = partes.length > 0 ? partes[0].trim() : semCidade;
        String numero = partes.length > 1 ? extrairNumero(partes[1]) : null;
        String bairro = partes.length > 2 ? partes[partes.length - 1].trim() : null;
        return new EnderecoTrabalhista(
                valorOuPadrao(logradouro, "Endereço descrito no edital oficial"),
                valorOuNulo(numero),
                valorOuNulo(bairro)
        );
    }

    private String extrairNumero(String texto) {
        Matcher matcher = Pattern.compile(
                "(?iu)(?:n[º°o.]?\\s*)?(\\d+[A-Za-z]?)"
        ).matcher(texto);
        return matcher.find() ? matcher.group(1) : null;
    }

    private LocalDateTime encerramentoDoLote(String texto, String identificador) {
        LocalDateTime encerramento = extrairData(texto);
        if (encerramento == null || identificador == null) {
            return encerramento;
        }

        Matcher intervalo = INTERVALO_ESCALONADO.matcher(texto);
        Matcher numeroLote = Pattern.compile("^(\\d{1,3})(?:\\.|$)")
                .matcher(identificador);
        if (intervalo.find() && numeroLote.find()) {
            int minutos = Integer.parseInt(intervalo.group(1));
            int lote = Integer.parseInt(numeroLote.group(1));
            return encerramento.plusMinutes((long) minutos * lote);
        }
        return encerramento;
    }

    private LocalDateTime extrairData(String texto) {
        Matcher numerica = DATA_NUMERICA.matcher(texto);
        if (numerica.find()) {
            return LocalDateTime.of(
                    Integer.parseInt(numerica.group(3)),
                    Integer.parseInt(numerica.group(2)),
                    Integer.parseInt(numerica.group(1)),
                    Integer.parseInt(numerica.group(4)),
                    Integer.parseInt(numerica.group(5))
            );
        }

        Matcher extenso = DATA_EXTENSO.matcher(texto);
        if (extenso.find()) {
            Month mes = mes(extenso.group(2));
            if (mes != null) {
                return LocalDateTime.of(
                        Integer.parseInt(extenso.group(3)),
                        mes,
                        Integer.parseInt(extenso.group(1)),
                        Integer.parseInt(extenso.group(4)),
                        Integer.parseInt(extenso.group(5))
                );
            }
        }
        return null;
    }

    private Month mes(String nomeMes) {
        return switch (normalizar(nomeMes)) {
            case "janeiro" -> Month.JANUARY;
            case "fevereiro" -> Month.FEBRUARY;
            case "marco" -> Month.MARCH;
            case "abril" -> Month.APRIL;
            case "maio" -> Month.MAY;
            case "junho" -> Month.JUNE;
            case "julho" -> Month.JULY;
            case "agosto" -> Month.AUGUST;
            case "setembro" -> Month.SEPTEMBER;
            case "outubro" -> Month.OCTOBER;
            case "novembro" -> Month.NOVEMBER;
            case "dezembro" -> Month.DECEMBER;
            default -> null;
        };
    }

    private String definirStatus(
            String texto,
            boolean ativo,
            LocalDateTime encerramento
    ) {
        String normalizado = normalizar(texto);
        if (!ativo || normalizado.contains("leilao cancelado")) return "CANCELADO";
        if (normalizado.contains("leilao suspenso")) return "SUSPENSO";
        if (encerramento == null) return "DESCONHECIDO";
        return LocalDateTime.now(clock).isAfter(encerramento)
                ? "ENCERRADO"
                : "AGENDADO";
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

    private Integer calcularDesconto(
            BigDecimal avaliacao,
            BigDecimal lanceMinimo,
            Integer percentualLance
    ) {
        if (avaliacao != null
                && avaliacao.signum() > 0
                && lanceMinimo != null) {
            return BigDecimal.ONE
                    .subtract(lanceMinimo.divide(avaliacao, 6, RoundingMode.HALF_UP))
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValue();
        }
        return percentualLance == null ? null : Math.max(0, 100 - percentualLance);
    }

    private BigDecimal extrairComissao(String texto) {
        Matcher rotulo = COMISSAO.matcher(texto);
        if (!rotulo.find()) {
            return null;
        }
        String trecho = texto.substring(
                rotulo.end(),
                Math.min(texto.length(), rotulo.end() + 80)
        );
        Matcher percentual = PERCENTUAL.matcher(trecho);
        return percentual.find()
                ? new BigDecimal(percentual.group(1).replace(',', '.'))
                : null;
    }

    private BigDecimal extrairValor(String texto, Pattern pattern) {
        Matcher matcher = pattern.matcher(texto);
        BigDecimal ultimo = null;
        while (matcher.find()) {
            ultimo = new BigDecimal(
                    matcher.group(1).replace(".", "").replace(',', '.')
            );
        }
        return ultimo;
    }

    private Integer extrairInteiro(String texto, Pattern pattern) {
        Matcher matcher = pattern.matcher(texto);
        if (!matcher.find()) {
            return null;
        }
        int valor = Integer.parseInt(matcher.group(1));
        return valor > 0 && valor <= 100 ? valor : null;
    }

    private Map<String, String> parametrosBusca(
            LocalDate inicio,
            LocalDate fim,
            int pagina,
            String texto,
            String processo
    ) {
        Map<String, String> parametros = new LinkedHashMap<>();
        parametros.put("siglaTribunal", siglaTribunal);
        parametros.put("meio", "E");
        parametros.put("dataDisponibilizacaoInicio", inicio.toString());
        parametros.put("dataDisponibilizacaoFim", fim.toString());
        parametros.put("itensPorPagina", Integer.toString(ITENS_POR_PAGINA));
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
                .userAgent("JudicialPipeline/1.4 (+consulta-publica-trt)")
                .header("Accept", "application/json")
                .ignoreContentType(true)
                .timeout(timeoutMs)
                .maxBodySize(0)
                .execute()
                .body();
        return OBJECT_MAPPER.readTree(corpo);
    }

    private String urlComunicacao(
            String hash,
            String processoPai,
            String processoLote,
            String lote
    ) {
        return CONSULTA_BASE + "/" + hash
                + "?numeroProcesso=" + digitos(processoPai)
                + "&processoLote=" + digitos(processoLote)
                + "&lote=" + codificar(lote);
    }

    private String processoDoItem(JsonNode item) {
        String processo = digitos(item.path("numero_processo").asText(""));
        if (processo.length() == 20) {
            return formatarProcesso(processo);
        }
        Matcher matcher = PROCESSO_FORMATADO.matcher(
                limparHtml(item.path("texto").asText(""))
        );
        return matcher.find() ? matcher.group() : null;
    }

    private String formatarProcesso(String processo) {
        return processo.substring(0, 7) + "-" + processo.substring(7, 9)
                + "." + processo.substring(9, 13)
                + "." + processo.substring(13, 14)
                + "." + processo.substring(14, 16)
                + "." + processo.substring(16, 20);
    }

    private String limparHtml(String html) {
        Document documento = Jsoup.parse(html == null ? "" : html);
        documento.select("style, script, noscript").remove();
        return documento.text()
                .replace('‑', '-')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String parametro(URI uri, String nomeParametro) {
        if (uri == null || uri.getRawQuery() == null) {
            return null;
        }
        for (String parametro : uri.getRawQuery().split("&")) {
            String[] partes = parametro.split("=", 2);
            if (partes.length == 2 && nomeParametro.equalsIgnoreCase(partes[0])) {
                return URLDecoder.decode(partes[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private String hashDoCaminho(URI uri) {
        Matcher matcher = CAMINHO_COMUNICACAO.matcher(uri.getPath());
        return matcher.matches() ? matcher.group(1) : "";
    }

    private String grupoOuNulo(Pattern pattern, String texto, int grupo) {
        Matcher matcher = pattern.matcher(texto);
        return matcher.find() ? valorOuNulo(matcher.group(grupo)) : null;
    }

    private String capitalizarCidade(String cidade) {
        return "votorantim".equals(normalizar(cidade)) ? "Votorantim" : "Sorocaba";
    }

    private String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String digitos(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }

    private String normalizar(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private String valorOuNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private String valorOuPadrao(String valor, String padrao) {
        String normalizado = valorOuNulo(valor);
        return normalizado == null ? padrao : normalizado;
    }

    @FunctionalInterface
    public interface ApiClient {
        JsonNode buscar(Map<String, String> parametros) throws Exception;
    }

    private record InicioProcesso(
            int inicio,
            String identificador,
            String processo
    ) {
    }

    private record BlocoProcesso(
            String processo,
            String identificador,
            String conteudo
    ) {
    }

    private record InicioBem(
            int inicio,
            String identificador,
            String tipo
    ) {
    }

    private record BlocoBem(
            String identificador,
            String conteudo
    ) {
    }

    private record LoteTrabalhista(
            String processo,
            String identificador,
            String cidade,
            String tipo,
            String conteudo,
            String orgao,
            String url
    ) {
    }

    private record EnderecoTrabalhista(
            String logradouro,
            String numero,
            String bairro
    ) {
    }
}
