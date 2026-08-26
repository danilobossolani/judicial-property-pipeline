package br.com.bossolani.judicialpipeline.service;

import br.com.bossolani.judicialpipeline.integration.datajud.dto.DataJudProcessoDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.DadosDinamicosLeilaoDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteEnriquecidoDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteLeilaoDTO;
import br.com.bossolani.judicialpipeline.model.*;
import br.com.bossolani.judicialpipeline.repository.FonteRepository;
import br.com.bossolani.judicialpipeline.repository.ImovelRepository;
import br.com.bossolani.judicialpipeline.repository.LeilaoRepository;
import br.com.bossolani.judicialpipeline.repository.ProcessoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class PersistenciaLeilaoService {

    private final IntegracaoLeilaoService integracaoLeilaoService;
    private final ProcessoRepository processoRepository;
    private final ImovelRepository imovelRepository;
    private final LeilaoRepository leilaoRepository;
    private final FonteRepository fonteRepository;

    public PersistenciaLeilaoService(
            IntegracaoLeilaoService integracaoLeilaoService,
            ProcessoRepository processoRepository,
            ImovelRepository imovelRepository,
            LeilaoRepository leilaoRepository,
            FonteRepository fonteRepository
    ) {
        this.integracaoLeilaoService = integracaoLeilaoService;
        this.processoRepository = processoRepository;
        this.imovelRepository = imovelRepository;
        this.leilaoRepository = leilaoRepository;
        this.fonteRepository = fonteRepository;
    }

    @Transactional
    public Fonte coletarESalvar(String url) throws Exception {

        LoteEnriquecidoDTO coleta =
                integracaoLeilaoService.buscarLote(url);

        LoteLeilaoDTO lote = coleta.lote();
        DadosDinamicosLeilaoDTO dadosLeilao = coleta.leilao();
        DataJudProcessoDTO dadosProcesso = coleta.processo();

        /*
         * Se essa URL já existe, não criamos outro imóvel/leilão.
         * Atualizamos o conjunto existente.
         */
        Fonte fonteExistente =
                fonteRepository
                        .findByUrlOrigem(url)
                        .orElse(null);

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

            return fonteRepository.save(fonteExistente);
        }

        /*
         * Caso seja uma URL nova, procuramos primeiro
         * se o processo já existe.
         */
        String numeroProcesso =
                normalizarNumeroProcesso(
                        lote.getNumeroProcesso()
                );

        Processo processo =
                processoRepository
                        .findByNumeroProcesso(numeroProcesso)
                        .orElseGet(Processo::new);

        preencherProcesso(
                processo,
                lote,
                dadosProcesso
        );

        processo =
                processoRepository.save(processo);

        /*
         * Criamos o imóvel.
         */
        Imovel imovel = new Imovel();

        preencherImovel(
                imovel,
                lote,
                processo
        );

        imovel =
                imovelRepository.save(imovel);

        /*
         * Criamos o leilão.
         */
        Leilao leilao = new Leilao();

        preencherLeilao(
                leilao,
                dadosLeilao,
                imovel
        );

        leilao =
                leilaoRepository.save(leilao);

        /*
         * Finalmente registramos a fonte.
         */
        Fonte fonte = new Fonte();

        fonte.setTipo(
                FonteTipo.LEILOEIRO_OFICIAL
        );

        fonte.setOrigemNome(
                "Sublime Leilões"
        );

        fonte.setUrlOrigem(url);

        fonte.setDataCaptura(
                LocalDateTime.now()
        );

        fonte.setLeilao(leilao);

        return fonteRepository.save(fonte);
    }

    private void atualizarExistente(
            Fonte fonte,
            LoteLeilaoDTO lote,
            DadosDinamicosLeilaoDTO dadosLeilao,
            DataJudProcessoDTO dadosProcesso
    ) {

        Leilao leilao = fonte.getLeilao();

        if (leilao == null) {
            throw new IllegalStateException(
                    "Fonte existente sem leilão associado"
            );
        }

        Imovel imovel = leilao.getImovel();

        if (imovel == null) {
            throw new IllegalStateException(
                    "Leilão existente sem imóvel associado"
            );
        }

        Processo processo = imovel.getProcesso();

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

        processoRepository.save(processo);
        imovelRepository.save(imovel);
        leilaoRepository.save(leilao);
    }

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

    private void preencherImovel(
            Imovel imovel,
            LoteLeilaoDTO lote,
            Processo processo
    ) {

        imovel.setEndereco(
                lote.getEndereco()
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

        leilao.setImovel(imovel);
    }

    private StatusLeilao converterStatus(
            String status
    ) {

        if (status == null) {
            return StatusLeilao.DESCONHECIDO;
        }

        String normalizado =
                status.toUpperCase();

        if (normalizado.contains("ENCERRADO")) {
            return StatusLeilao.ENCERRADO;
        }

        if (normalizado.contains("SUSPENSO")) {
            return StatusLeilao.SUSPENSO;
        }

        if (normalizado.contains("CANCELADO")) {
            return StatusLeilao.CANCELADO;
        }

        if (normalizado.contains("ANDAMENTO")
                || normalizado.contains("ABERTO")) {

            return StatusLeilao.EM_ANDAMENTO;
        }

        return StatusLeilao.DESCONHECIDO;
    }

    private ResultadoLeilao converterResultado(
            String resultado
    ) {

        if (resultado == null) {
            return ResultadoLeilao.DESCONHECIDO;
        }

        String normalizado =
                resultado.toUpperCase();

        if (normalizado.contains("SEM LANCES")) {
            return ResultadoLeilao.SEM_LANCES;
        }

        if (normalizado.contains("ARREMATADO")) {
            return ResultadoLeilao.ARREMATADO;
        }

        if (normalizado.contains("DESERTO")) {
            return ResultadoLeilao.DESERTO;
        }

        if (normalizado.contains("COM LANCES")) {
            return ResultadoLeilao.COM_LANCES;
        }

        return ResultadoLeilao.DESCONHECIDO;
    }

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