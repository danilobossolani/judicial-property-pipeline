package br.com.bossolani.judicialpipeline.service;

import br.com.bossolani.judicialpipeline.integration.datajud.dto.DataJudProcessoDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.DadosDinamicosLeilaoDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteEnriquecidoDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteLeilaoDTO;
import br.com.bossolani.judicialpipeline.model.Acompanhamento;
import br.com.bossolani.judicialpipeline.model.Fonte;
import br.com.bossolani.judicialpipeline.model.FonteTipo;
import br.com.bossolani.judicialpipeline.model.HistoricoAcompanhamento;
import br.com.bossolani.judicialpipeline.model.Imovel;
import br.com.bossolani.judicialpipeline.model.Leilao;
import br.com.bossolani.judicialpipeline.model.Processo;
import br.com.bossolani.judicialpipeline.model.ResultadoLeilao;
import br.com.bossolani.judicialpipeline.model.StatusLeilao;
import br.com.bossolani.judicialpipeline.model.StatusPipeline;
import br.com.bossolani.judicialpipeline.repository.AcompanhamentoRepository;
import br.com.bossolani.judicialpipeline.repository.FonteRepository;
import br.com.bossolani.judicialpipeline.repository.HistoricoAcompanhamentoRepository;
import br.com.bossolani.judicialpipeline.repository.ImovelRepository;
import br.com.bossolani.judicialpipeline.repository.LeilaoRepository;
import br.com.bossolani.judicialpipeline.repository.ProcessoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PersistenciaLeilaoService {

    private static final int TAMANHO_NUMERO_PROCESSO_CNJ = 20;

    private final IntegracaoLeilaoService integracaoLeilaoService;
    private final ProcessoRepository processoRepository;
    private final ImovelRepository imovelRepository;
    private final LeilaoRepository leilaoRepository;
    private final FonteRepository fonteRepository;
    private final AcompanhamentoRepository acompanhamentoRepository;
    private final HistoricoAcompanhamentoRepository historicoAcompanhamentoRepository;
    private final TriagemLoteService triagemLoteService;


    public PersistenciaLeilaoService(
            IntegracaoLeilaoService integracaoLeilaoService,
            ProcessoRepository processoRepository,
            ImovelRepository imovelRepository,
            LeilaoRepository leilaoRepository,
            FonteRepository fonteRepository,
            AcompanhamentoRepository acompanhamentoRepository,
            HistoricoAcompanhamentoRepository historicoAcompanhamentoRepository,
            TriagemLoteService triagemLoteService
    ) {

        this.integracaoLeilaoService = integracaoLeilaoService;
        this.processoRepository = processoRepository;
        this.imovelRepository = imovelRepository;
        this.leilaoRepository = leilaoRepository;
        this.fonteRepository = fonteRepository;
        this.acompanhamentoRepository = acompanhamentoRepository;
        this.historicoAcompanhamentoRepository =
                historicoAcompanhamentoRepository;
        this.triagemLoteService = triagemLoteService;
    }


    @Transactional
    public Fonte coletarESalvar(
            String url
    ) throws Exception {

        String urlNormalizada =
                triagemLoteService.normalizarUrl(
                        url
                );


        LoteEnriquecidoDTO coleta =
                integracaoLeilaoService.buscarLote(
                        urlNormalizada
                );


        LoteLeilaoDTO lote =
                coleta.lote();


        DadosDinamicosLeilaoDTO dadosLeilao =
                coleta.leilao();


        DataJudProcessoDTO dadosProcesso =
                coleta.processo();


        validarColeta(
                lote,
                dadosLeilao
        );


        Fonte fonteExistente =
                fonteRepository
                        .findByUrlOrigem(
                                urlNormalizada
                        )
                        .orElse(null);


        /*
         * Deduplicação primária por URL canônica.
         * Uma nova captura atualiza o mesmo conjunto existente.
         */
        if (fonteExistente != null) {

            atualizarExistente(
                    fonteExistente,
                    lote,
                    dadosLeilao,
                    dadosProcesso
            );


            fonteExistente.setDataCaptura(
                    LocalDateTime.now()
            );


            return fonteRepository.save(
                    fonteExistente
            );
        }


        String numeroProcesso =
                normalizarEValidarNumeroProcesso(
                        lote.getNumeroProcesso()
                );


        Optional<Processo> processoExistente =
                processoRepository
                        .findByNumeroProcesso(
                                numeroProcesso
                        );


        Processo processo =
                processoExistente
                        .orElseGet(
                                Processo::new
                        );


        preencherProcesso(
                processo,
                lote,
                dadosProcesso
        );


        processo =
                processoRepository.save(
                        processo
                );


        /*
         * Deduplicação secundária por processo.
         * Se outra URL apontar para o processo já cadastrado,
         * ela vira uma nova fonte do mesmo imóvel/leilão.
         */
        Optional<Imovel> imovelExistente =
                imovelRepository
                        .findFirstByProcessoIdOrderByIdAsc(
                                processo.getId()
                        );


        Imovel imovel =
                imovelExistente
                        .orElseGet(
                                Imovel::new
                        );


        preencherImovel(
                imovel,
                lote,
                processo
        );


        imovel =
                imovelRepository.save(
                        imovel
                );


        Optional<Leilao> leilaoExistente =
                imovelExistente.isPresent()
                        ? leilaoRepository
                        .findTopByImovelIdOrderByIdDesc(
                                imovel.getId()
                        )
                        : Optional.empty();


        Leilao leilao =
                leilaoExistente
                        .orElseGet(
                                Leilao::new
                        );


        preencherLeilao(
                leilao,
                dadosLeilao,
                imovel
        );


        leilao =
                leilaoRepository.save(
                        leilao
                );


        atualizarAcompanhamento(
                imovel,
                leilao
        );


        Fonte fonte =
                new Fonte();


        fonte.setTipo(
                FonteTipo.LEILOEIRO_OFICIAL
        );


        fonte.setOrigemNome(
                "Sublime Leilões"
        );


        fonte.setUrlOrigem(
                urlNormalizada
        );


        fonte.setDataCaptura(
                LocalDateTime.now()
        );


        fonte.setLeilao(
                leilao
        );


        return fonteRepository.save(
                fonte
        );
    }


    private void validarColeta(
            LoteLeilaoDTO lote,
            DadosDinamicosLeilaoDTO dadosLeilao
    ) {

        if (lote == null) {

            throw new IllegalArgumentException(
                    "A Sublime não retornou os dados do lote"
            );
        }


        if (dadosLeilao == null) {

            throw new IllegalArgumentException(
                    "A Sublime não retornou os dados do leilão"
            );
        }


        normalizarEValidarNumeroProcesso(
                lote.getNumeroProcesso()
        );
    }


    private void atualizarExistente(
            Fonte fonte,
            LoteLeilaoDTO lote,
            DadosDinamicosLeilaoDTO dadosLeilao,
            DataJudProcessoDTO dadosProcesso
    ) {

        Leilao leilao =
                fonte.getLeilao();


        if (leilao == null) {

            throw new IllegalStateException(
                    "Fonte existente sem leilão associado"
            );
        }


        Imovel imovel =
                leilao.getImovel();


        if (imovel == null) {

            throw new IllegalStateException(
                    "Leilão existente sem imóvel associado"
            );
        }


        Processo processo =
                imovel.getProcesso();


        if (processo == null) {

            throw new IllegalStateException(
                    "Imóvel existente sem processo associado"
            );
        }


        preencherProcesso(
                processo,
                lote,
                dadosProcesso
        );


        preencherImovel(
                imovel,
                lote,
                processo
        );


        preencherLeilao(
                leilao,
                dadosLeilao,
                imovel
        );


        processoRepository.save(
                processo
        );


        imovelRepository.save(
                imovel
        );


        leilaoRepository.save(
                leilao
        );


        atualizarAcompanhamento(
                imovel,
                leilao
        );
    }


    private void atualizarAcompanhamento(
            Imovel imovel,
            Leilao leilao
    ) {

        Acompanhamento acompanhamento =
                acompanhamentoRepository
                        .findByImovelId(
                                imovel.getId()
                        )
                        .orElseGet(
                                Acompanhamento::new
                        );


        LocalDateTime agora =
                LocalDateTime.now();


        boolean novoAcompanhamento =
                acompanhamento.getId() == null;


        if (novoAcompanhamento) {

            acompanhamento.setImovel(
                    imovel
            );


            acompanhamento.setDataIdentificacao(
                    agora
            );
        }


        StatusPipeline statusAutomatico =
                definirStatusPipeline(
                        leilao
                );


        StatusPipeline statusFinal =
                resolverStatusPipeline(
                        acompanhamento,
                        statusAutomatico
                );


        acompanhamento.setStatusPipeline(
                statusFinal
        );


        acompanhamento.setUltimaVerificacao(
                agora
        );


        acompanhamento.setAtivo(
                statusFinal != StatusPipeline.ENCERRADO
                        && statusFinal != StatusPipeline.DESCARTADO
        );


        acompanhamento =
                acompanhamentoRepository.save(
                        acompanhamento
                );


        verificarHistorico(
                acompanhamento,
                leilao,
                statusFinal
        );
    }


    private StatusPipeline resolverStatusPipeline(
            Acompanhamento acompanhamento,
            StatusPipeline statusAutomatico
    ) {

        StatusPipeline statusAtual =
                acompanhamento.getStatusPipeline();


        if (statusAtual == null) {
            return statusAutomatico;
        }


        if (statusAutomatico == StatusPipeline.ENCERRADO
                && (
                statusAtual == StatusPipeline.EM_ANALISE
                        || statusAtual == StatusPipeline.OPORTUNIDADE
        )) {

            return StatusPipeline.ENCERRADO;
        }


        if (statusOperacionalProtegido(
                statusAtual
        )) {

            return statusAtual;
        }


        return statusAutomatico;
    }


    private boolean statusOperacionalProtegido(
            StatusPipeline status
    ) {

        if (status == null) {
            return false;
        }


        return switch (status) {

            case EM_ANALISE,
                 OPORTUNIDADE,
                 DESCARTADO,
                 ENCERRADO -> true;

            default -> false;
        };
    }


    private void verificarHistorico(
            Acompanhamento acompanhamento,
            Leilao leilao,
            StatusPipeline statusPipelineAtual
    ) {

        Optional<HistoricoAcompanhamento> ultimoHistoricoOptional =
                historicoAcompanhamentoRepository
                        .findTopByAcompanhamentoIdOrderByDataEventoDesc(
                                acompanhamento.getId()
                        );


        if (ultimoHistoricoOptional.isEmpty()) {

            registrarHistorico(
                    acompanhamento,
                    leilao,
                    statusPipelineAtual
            );


            return;
        }


        HistoricoAcompanhamento ultimoHistorico =
                ultimoHistoricoOptional.get();


        if (ultimoHistorico.getStatusLeilao() == null
                && ultimoHistorico.getResultadoLeilao() == null) {

            ultimoHistorico.setStatusLeilao(
                    leilao.getStatusLeilao()
            );


            ultimoHistorico.setResultadoLeilao(
                    leilao.getResultadoLeilao()
            );


            historicoAcompanhamentoRepository.save(
                    ultimoHistorico
            );


            return;
        }


        boolean mudouPipeline =
                ultimoHistorico.getStatusPipeline()
                        != statusPipelineAtual;


        boolean mudouStatusLeilao =
                ultimoHistorico.getStatusLeilao()
                        != leilao.getStatusLeilao();


        boolean mudouResultadoLeilao =
                ultimoHistorico.getResultadoLeilao()
                        != leilao.getResultadoLeilao();


        if (mudouPipeline
                || mudouStatusLeilao
                || mudouResultadoLeilao) {

            registrarHistorico(
                    acompanhamento,
                    leilao,
                    statusPipelineAtual
            );
        }
    }


    private void registrarHistorico(
            Acompanhamento acompanhamento,
            Leilao leilao,
            StatusPipeline statusPipeline
    ) {

        HistoricoAcompanhamento historico =
                new HistoricoAcompanhamento();


        historico.setAcompanhamento(
                acompanhamento
        );


        historico.setDataEvento(
                LocalDateTime.now()
        );


        historico.setStatusPipeline(
                statusPipeline
        );


        historico.setStatusLeilao(
                leilao.getStatusLeilao()
        );


        historico.setResultadoLeilao(
                leilao.getResultadoLeilao()
        );


        historico.setOrigem(
                "Sublime Leilões"
        );


        String descricao =
                "Leilão: "
                        + leilao.getStatusLeilao()
                        + " | Resultado: "
                        + leilao.getResultadoLeilao();


        historico.setDescricao(
                descricao
        );


        historicoAcompanhamentoRepository.save(
                historico
        );
    }


    private StatusPipeline definirStatusPipeline(
            Leilao leilao
    ) {

        if (leilao.getStatusLeilao()
                == StatusLeilao.AGENDADO) {

            return StatusPipeline.MONITORANDO_LEILAO;
        }


        if (leilao.getStatusLeilao()
                == StatusLeilao.EM_ANDAMENTO) {

            return StatusPipeline.MONITORANDO_LEILAO;
        }


        if (leilao.getStatusLeilao()
                == StatusLeilao.ENCERRADO) {

            if (leilao.getResultadoLeilao()
                    == ResultadoLeilao.ARREMATADO) {

                return StatusPipeline.ENCERRADO;
            }


            if (leilao.getResultadoLeilao()
                    == ResultadoLeilao.DESCONHECIDO) {

                return StatusPipeline.AGUARDANDO_RESULTADO;
            }


            return StatusPipeline.MONITORANDO_PROCESSO;
        }


        return StatusPipeline.IDENTIFICADO;
    }


    private void preencherProcesso(
            Processo processo,
            LoteLeilaoDTO lote,
            DataJudProcessoDTO dadosProcesso
    ) {

        processo.setNumeroProcesso(
                normalizarEValidarNumeroProcesso(
                        lote.getNumeroProcesso()
                )
        );


        processo.setComarca(
                lote.getComarca()
        );


        processo.setVara(
                lote.getVara()
        );


        if (dadosProcesso != null) {

            processo.setTribunal(
                    dadosProcesso.tribunal()
            );


            processo.setGrau(
                    dadosProcesso.grau()
            );


            processo.setOrgaoJulgador(
                    dadosProcesso.orgaoJulgador()
            );


            processo.setClasse(
                    dadosProcesso.classe()
            );


            processo.setSistema(
                    dadosProcesso.sistema()
            );


            processo.setFormato(
                    dadosProcesso.formato()
            );


            processo.setDataAjuizamento(
                    dadosProcesso.dataAjuizamento()
            );


            processo.setUltimaAtualizacao(
                    dadosProcesso.ultimaAtualizacao()
            );
        }
    }


    private void preencherImovel(
            Imovel imovel,
            LoteLeilaoDTO lote,
            Processo processo
    ) {

        imovel.setTipo(
                lote.getTipo()
        );


        imovel.setEndereco(
                lote.getEndereco()
        );


        imovel.setNumero(
                lote.getNumero()
        );


        imovel.setBairro(
                lote.getBairro()
        );


        imovel.setCidade(
                lote.getComarca()
        );


        imovel.setValorAvaliacao(
                lote.getValorAvaliacao()
        );


        imovel.setProcesso(
                processo
        );
    }


    private void preencherLeilao(
            Leilao leilao,
            DadosDinamicosLeilaoDTO dados,
            Imovel imovel
    ) {

        leilao.setAbertura1Praca(
                dados.abertura1Praca()
        );


        leilao.setFechamento1Praca(
                dados.fechamento1Praca()
        );


        leilao.setLanceInicial1Praca(
                dados.lanceInicial1Praca()
        );


        leilao.setAbertura2Praca(
                dados.abertura2Praca()
        );


        leilao.setFechamento2Praca(
                dados.fechamento2Praca()
        );


        leilao.setLanceInicial2Praca(
                dados.lanceInicial2Praca()
        );


        leilao.setPercentualDescontoFonte(
                dados.percentualDesconto()
        );


        leilao.setLanceMinimo(
                dados.lanceMinimo()
        );


        leilao.setIncremento(
                dados.incremento()
        );


        leilao.setComissaoPercentual(
                dados.comissaoPercentual()
        );


        leilao.setStatusLeilao(
                converterStatus(
                        dados.status()
                )
        );


        leilao.setResultadoLeilao(
                converterResultado(
                        dados.resultado()
                )
        );


        leilao.setImovel(
                imovel
        );
    }


    private StatusLeilao converterStatus(
            String status
    ) {

        if (status == null
                || status.isBlank()) {

            return StatusLeilao.DESCONHECIDO;
        }


        String normalizado =
                status.toUpperCase();


        if (normalizado.contains(
                "ENCERRADO"
        )) {

            return StatusLeilao.ENCERRADO;
        }


        if (normalizado.contains(
                "SUSPENSO"
        )) {

            return StatusLeilao.SUSPENSO;
        }


        if (normalizado.contains(
                "CANCELADO"
        )) {

            return StatusLeilao.CANCELADO;
        }


        if (normalizado.contains(
                "AGENDADO"
        )
                || normalizado.contains(
                "EM BREVE"
        )
                || normalizado.contains(
                "AGUARDANDO INÍCIO"
        )
                || normalizado.contains(
                "AGUARDANDO INICIO"
        )) {

            return StatusLeilao.AGENDADO;
        }


        if (normalizado.contains(
                "ANDAMENTO"
        )
                || normalizado.contains(
                "ABERTO"
        )) {

            return StatusLeilao.EM_ANDAMENTO;
        }


        return StatusLeilao.DESCONHECIDO;
    }


    private ResultadoLeilao converterResultado(
            String resultado
    ) {

        if (resultado == null
                || resultado.isBlank()) {

            return ResultadoLeilao.DESCONHECIDO;
        }


        String normalizado =
                resultado.toUpperCase();


        if (normalizado.contains(
                "SEM LANCES"
        )) {

            return ResultadoLeilao.SEM_LANCES;
        }


        if (normalizado.contains(
                "ARREMATADO"
        )) {

            return ResultadoLeilao.ARREMATADO;
        }


        if (normalizado.contains(
                "DESERTO"
        )) {

            return ResultadoLeilao.DESERTO;
        }


        if (normalizado.contains(
                "COM LANCES"
        )) {

            return ResultadoLeilao.COM_LANCES;
        }


        return ResultadoLeilao.DESCONHECIDO;
    }


    private String normalizarEValidarNumeroProcesso(
            String numeroProcesso
    ) {

        String numeroNormalizado =
                numeroProcesso == null
                        ? ""
                        : numeroProcesso.replaceAll(
                                "\\D",
                                ""
                        );


        if (numeroNormalizado.length()
                != TAMANHO_NUMERO_PROCESSO_CNJ) {

            throw new IllegalArgumentException(
                    "Lote sem número de processo CNJ válido"
            );
        }


        return numeroNormalizado;
    }
}
