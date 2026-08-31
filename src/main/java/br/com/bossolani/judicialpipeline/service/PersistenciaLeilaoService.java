package br.com.bossolani.judicialpipeline.service;

import br.com.bossolani.judicialpipeline.integration.datajud.dto.DataJudProcessoDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.DadosDinamicosLeilaoDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteEnriquecidoDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteLeilaoDTO;
import br.com.bossolani.judicialpipeline.model.*;
import br.com.bossolani.judicialpipeline.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PersistenciaLeilaoService {

    private final IntegracaoLeilaoService integracaoLeilaoService;
    private final ProcessoRepository processoRepository;
    private final ImovelRepository imovelRepository;
    private final LeilaoRepository leilaoRepository;
    private final FonteRepository fonteRepository;
    private final AcompanhamentoRepository acompanhamentoRepository;
    private final HistoricoAcompanhamentoRepository historicoAcompanhamentoRepository;


    public PersistenciaLeilaoService(
            IntegracaoLeilaoService integracaoLeilaoService,
            ProcessoRepository processoRepository,
            ImovelRepository imovelRepository,
            LeilaoRepository leilaoRepository,
            FonteRepository fonteRepository,
            AcompanhamentoRepository acompanhamentoRepository,
            HistoricoAcompanhamentoRepository historicoAcompanhamentoRepository
    ) {

        this.integracaoLeilaoService = integracaoLeilaoService;
        this.processoRepository = processoRepository;
        this.imovelRepository = imovelRepository;
        this.leilaoRepository = leilaoRepository;
        this.fonteRepository = fonteRepository;
        this.acompanhamentoRepository = acompanhamentoRepository;
        this.historicoAcompanhamentoRepository =
                historicoAcompanhamentoRepository;
    }


    @Transactional
    public Fonte coletarESalvar(
            String url
    ) throws Exception {

        LoteEnriquecidoDTO coleta =
                integracaoLeilaoService.buscarLote(
                        url
                );


        LoteLeilaoDTO lote =
                coleta.lote();


        DadosDinamicosLeilaoDTO dadosLeilao =
                coleta.leilao();


        DataJudProcessoDTO dadosProcesso =
                coleta.processo();


        Fonte fonteExistente =
                fonteRepository
                        .findByUrlOrigem(url)
                        .orElse(null);


        /*
         * Se a fonte já existe, atualizamos o mesmo conjunto
         * Processo -> Imóvel -> Leilão -> Acompanhamento.
         *
         * Dessa forma uma nova coleta não cria duplicatas.
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


        /*
         * NOVA COLETA
         */

        String numeroProcesso =
                normalizarNumeroProcesso(
                        lote.getNumeroProcesso()
                );


        Processo processo =
                processoRepository
                        .findByNumeroProcesso(
                                numeroProcesso
                        )
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
         * IMÓVEL
         */

        Imovel imovel =
                new Imovel();


        preencherImovel(
                imovel,
                lote,
                processo
        );


        imovel =
                imovelRepository.save(
                        imovel
                );


        /*
         * LEILÃO
         */

        Leilao leilao =
                new Leilao();


        preencherLeilao(
                leilao,
                dadosLeilao,
                imovel
        );


        leilao =
                leilaoRepository.save(
                        leilao
                );


        /*
         * PIPELINE
         */

        atualizarAcompanhamento(
                imovel,
                leilao
        );


        /*
         * FONTE
         */

        Fonte fonte =
                new Fonte();


        fonte.setTipo(
                FonteTipo.LEILOEIRO_OFICIAL
        );


        fonte.setOrigemNome(
                "Sublime Leilões"
        );


        fonte.setUrlOrigem(
                url
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


    /*
     * =========================================================
     * ATUALIZAÇÃO DE UMA COLETA EXISTENTE
     * =========================================================
     */

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


    /*
     * =========================================================
     * ACOMPANHAMENTO / PIPELINE
     * =========================================================
     */

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


        /*
         * Este é o status que a coleta sugeriria
         * automaticamente com base nos fatos do leilão.
         */
        StatusPipeline statusAutomatico =
                definirStatusPipeline(
                        leilao
                );


        /*
         * Aqui protegemos decisões humanas.
         *
         * Exemplo:
         *
         * OPORTUNIDADE
         *      ↓
         * nova coleta
         *      ↓
         * continua OPORTUNIDADE
         *
         * Porém, se o imóvel for posteriormente ARREMATADO,
         * ele deve ser encerrado mesmo que estivesse em análise
         * ou marcado como oportunidade.
         */
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


        /*
         * Mesmo se o status operacional estiver protegido,
         * alterações factuais do leilão continuam entrando
         * normalmente no histórico.
         */
        verificarHistorico(
                acompanhamento,
                leilao,
                statusFinal
        );
    }


    /*
     * Decide se a coleta automática pode ou não
     * modificar o status operacional atual.
     */
    private StatusPipeline resolverStatusPipeline(
            Acompanhamento acompanhamento,
            StatusPipeline statusAutomatico
    ) {

        StatusPipeline statusAtual =
                acompanhamento.getStatusPipeline();


        /*
         * Acompanhamento ainda sem status.
         */
        if (statusAtual == null) {

            return statusAutomatico;
        }


        /*
         * Se descobrirmos que houve arrematação,
         * o imóvel deixa de ser oportunidade para nós.
         *
         * EM_ANALISE e OPORTUNIDADE podem ser encerrados
         * automaticamente nesse caso.
         */
        if (statusAutomatico == StatusPipeline.ENCERRADO
                && (
                statusAtual == StatusPipeline.EM_ANALISE
                        || statusAtual == StatusPipeline.OPORTUNIDADE
        )) {

            return StatusPipeline.ENCERRADO;
        }


        /*
         * Decisões humanas são protegidas contra
         * atualizações automáticas comuns.
         */
        if (statusOperacionalProtegido(
                statusAtual
        )) {

            return statusAtual;
        }


        /*
         * Status ainda controlado automaticamente.
         */
        return statusAutomatico;
    }


    /*
     * Status considerados decisões operacionais humanas.
     */
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


    /*
     * =========================================================
     * HISTÓRICO
     * =========================================================
     */

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


        /*
         * Primeiro evento.
         */
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


        /*
         * Compatibilidade com registros antigos criados
         * antes de statusLeilao e resultadoLeilao existirem
         * no histórico.
         */
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


        /*
         * Não criamos histórico novo sem mudança real.
         */
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


    /*
     * =========================================================
     * DEFINIÇÃO AUTOMÁTICA DO PIPELINE
     * =========================================================
     */

    private StatusPipeline definirStatusPipeline(
            Leilao leilao
    ) {

        /*
         * Leilão ainda vai acontecer.
         */
        if (leilao.getStatusLeilao()
                == StatusLeilao.AGENDADO) {

            return StatusPipeline.MONITORANDO_LEILAO;
        }


        /*
         * Leilão acontecendo.
         */
        if (leilao.getStatusLeilao()
                == StatusLeilao.EM_ANDAMENTO) {

            return StatusPipeline.MONITORANDO_LEILAO;
        }


        /*
         * Leilão finalizado.
         */
        if (leilao.getStatusLeilao()
                == StatusLeilao.ENCERRADO) {


            /*
             * Já foi comprado por alguém.
             */
            if (leilao.getResultadoLeilao()
                    == ResultadoLeilao.ARREMATADO) {

                return StatusPipeline.ENCERRADO;
            }


            /*
             * Terminou, mas ainda não sabemos o resultado.
             */
            if (leilao.getResultadoLeilao()
                    == ResultadoLeilao.DESCONHECIDO) {

                return StatusPipeline.AGUARDANDO_RESULTADO;
            }


            /*
             * Sem arrematação confirmada.
             *
             * A partir daqui acompanhamos o processo judicial,
             * mas ainda NÃO chamamos automaticamente de
             * oportunidade.
             */
            return StatusPipeline.MONITORANDO_PROCESSO;
        }


        return StatusPipeline.IDENTIFICADO;
    }


    /*
     * =========================================================
     * PROCESSO
     * =========================================================
     */

    private void preencherProcesso(
            Processo processo,
            LoteLeilaoDTO lote,
            DataJudProcessoDTO dadosProcesso
    ) {

        processo.setNumeroProcesso(
                normalizarNumeroProcesso(
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


    /*
     * =========================================================
     * IMÓVEL
     * =========================================================
     */

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


        /*
         * Hoje o scraper da Sublime usa a comarca como cidade.
         *
         * Para Sorocaba/Votorantim isso funciona nos casos
         * atuais, mas futuramente devemos extrair a cidade
         * diretamente do endereço quando adicionarmos outras
         * fontes.
         */
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


    /*
     * =========================================================
     * LEILÃO
     * =========================================================
     */

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


    /*
     * =========================================================
     * CONVERSÃO DO STATUS DA FONTE
     * =========================================================
     */

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


    /*
     * =========================================================
     * CONVERSÃO DO RESULTADO
     * =========================================================
     */

    private ResultadoLeilao converterResultado(
            String resultado
    ) {

        if (resultado == null
                || resultado.isBlank()) {

            return ResultadoLeilao.DESCONHECIDO;
        }


        String normalizado =
                resultado.toUpperCase();


        /*
         * A ordem é importante.
         *
         * "SEM LANCES" contém a palavra "LANCES", então
         * precisa ser verificado antes de "COM LANCES".
         */
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


    /*
     * =========================================================
     * PROCESSO CNJ
     * =========================================================
     */

    private String normalizarNumeroProcesso(
            String numeroProcesso
    ) {

        if (numeroProcesso == null) {
            return "";
        }


        return numeroProcesso.replaceAll(
                "\\D",
                ""
        );
    }
}