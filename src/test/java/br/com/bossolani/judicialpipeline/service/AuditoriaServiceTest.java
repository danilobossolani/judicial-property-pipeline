package br.com.bossolani.judicialpipeline.service;

import br.com.bossolani.judicialpipeline.dto.CentralAuditoriaDTO;
import br.com.bossolani.judicialpipeline.dto.EventoAuditoriaDTO;
import br.com.bossolani.judicialpipeline.model.Acompanhamento;
import br.com.bossolani.judicialpipeline.model.DecisaoLoteDescoberta;
import br.com.bossolani.judicialpipeline.model.ExecucaoDescoberta;
import br.com.bossolani.judicialpipeline.model.HistoricoAcompanhamento;
import br.com.bossolani.judicialpipeline.model.Imovel;
import br.com.bossolani.judicialpipeline.model.OrigemExecucaoDescoberta;
import br.com.bossolani.judicialpipeline.model.Processo;
import br.com.bossolani.judicialpipeline.model.ResultadoLeilao;
import br.com.bossolani.judicialpipeline.model.ResultadoLoteDescoberta;
import br.com.bossolani.judicialpipeline.model.StatusExecucaoDescoberta;
import br.com.bossolani.judicialpipeline.model.StatusLeilao;
import br.com.bossolani.judicialpipeline.model.StatusPipeline;
import br.com.bossolani.judicialpipeline.repository.ExecucaoDescobertaRepository;
import br.com.bossolani.judicialpipeline.repository.HistoricoAcompanhamentoRepository;
import br.com.bossolani.judicialpipeline.repository.ResultadoLoteDescobertaRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuditoriaServiceTest {

    @Test
    void deveConsolidarEventosManuaisEAutomaticosPorImovel() {

        HistoricoAcompanhamentoRepository repository =
                mock(
                        HistoricoAcompanhamentoRepository.class
                );

        ExecucaoDescobertaRepository execucaoRepository =
                mock(
                        ExecucaoDescobertaRepository.class
                );

        ResultadoLoteDescobertaRepository resultadoRepository =
                mock(
                        ResultadoLoteDescobertaRepository.class
                );


        LocalDateTime dataMaisRecente =
                LocalDateTime.of(
                        2026,
                        8,
                        30,
                        21,
                        15
                );


        HistoricoAcompanhamento manual =
                criarHistorico(
                        10L,
                        dataMaisRecente,
                        "Atualização manual",
                        StatusPipeline.OPORTUNIDADE,
                        "Observação: matrícula conferida."
                );

        HistoricoAcompanhamento automaticoMesmoImovel =
                criarHistorico(
                        10L,
                        dataMaisRecente.minusHours(2),
                        "Sublime Leilões",
                        StatusPipeline.EM_ANALISE,
                        "Leilão: ENCERRADO | Resultado: SEM_LANCES"
                );

        HistoricoAcompanhamento automaticoOutroImovel =
                criarHistorico(
                        20L,
                        dataMaisRecente.minusDays(1),
                        "Sublime Leilões",
                        StatusPipeline.MONITORANDO_LEILAO,
                        "Leilão: AGENDADO | Resultado: DESCONHECIDO"
                );


        when(repository.findTop200ByOrderByDataEventoDesc())
                .thenReturn(
                        List.of(
                                manual,
                                automaticoMesmoImovel,
                                automaticoOutroImovel
                        )
                );

        when(execucaoRepository.findTop20ByOrderByInicioDesc())
                .thenReturn(
                        List.of()
                );


        AuditoriaService service =
                new AuditoriaService(
                        repository,
                        execucaoRepository,
                        resultadoRepository
                );


        CentralAuditoriaDTO central =
                service.carregarCentral();


        assertEquals(
                3,
                central.totalEventos()
        );

        assertEquals(
                1,
                central.totalManuais()
        );

        assertEquals(
                2,
                central.totalAutomaticos()
        );

        assertEquals(
                2,
                central.totalImoveisImpactados()
        );

        assertEquals(
                dataMaisRecente,
                central.ultimaAtividade()
        );

        assertNull(
                central.descoberta()
                        .ultimaExecucao()
        );


        EventoAuditoriaDTO primeiroEvento =
                central.eventos().getFirst();


        assertEquals(
                "MANUAL",
                primeiroEvento.categoria()
        );

        assertEquals(
                "1234567-89.2026.8.26.0602",
                primeiroEvento.numeroProcessoFormatado()
        );

        assertEquals(
                "Centro — Sorocaba",
                primeiroEvento.localizacaoFormatada()
        );
    }

    @Test
    void deveConsolidarIndicadoresELotesDaDescoberta() {

        HistoricoAcompanhamentoRepository historicoRepository =
                mock(
                        HistoricoAcompanhamentoRepository.class
                );

        ExecucaoDescobertaRepository execucaoRepository =
                mock(
                        ExecucaoDescobertaRepository.class
                );

        ResultadoLoteDescobertaRepository resultadoRepository =
                mock(
                        ResultadoLoteDescobertaRepository.class
                );


        ExecucaoDescoberta execucao =
                new ExecucaoDescoberta();

        execucao.setFonte(
                "Sublime Leilões"
        );

        execucao.setInicio(
                LocalDateTime.of(
                        2026,
                        8,
                        31,
                        8,
                        30
                )
        );

        execucao.setOrigem(
                OrigemExecucaoDescoberta.AGENDADA
        );

        execucao.setStatus(
                StatusExecucaoDescoberta.CONCLUIDA_COM_FALHAS
        );

        execucao.setTotalEncontrado(4);
        execucao.setTotalElegivel(3);
        execucao.setTotalImportado(1);
        execucao.setTotalDuplicado(1);
        execucao.setTotalDescartado(1);
        execucao.setTotalFalha(1);


        ResultadoLoteDescoberta lote =
                new ResultadoLoteDescoberta();

        lote.setTitulo(
                "Casa em Sorocaba"
        );

        lote.setCidade(
                "Sorocaba"
        );

        lote.setUrlOriginal(
                "https://www.sublimeleiloes.com.br/lote/casa-em-sorocaba/2405/?origem=busca"
        );

        lote.setUrlNormalizada(
                "https://www.sublimeleiloes.com.br/lote/casa-em-sorocaba/2405/"
        );

        lote.setNumeroProcesso(
                "00467199720118260602"
        );

        lote.setDecisao(
                DecisaoLoteDescoberta.IMPORTADO
        );

        lote.setMotivo(
                "Lote elegível importado e relacionado ao imóvel."
        );


        when(historicoRepository.findTop200ByOrderByDataEventoDesc())
                .thenReturn(
                        List.of()
                );

        when(execucaoRepository.findTop20ByOrderByInicioDesc())
                .thenReturn(
                        List.of(
                                execucao
                        )
                );

        when(resultadoRepository.findByExecucaoIdOrderByIdAsc(
                null
        )).thenReturn(
                List.of(
                        lote
                )
        );


        AuditoriaService service =
                new AuditoriaService(
                        historicoRepository,
                        execucaoRepository,
                        resultadoRepository
                );


        CentralAuditoriaDTO central =
                service.carregarCentral();


        assertEquals(
                1,
                central.descoberta()
                        .totalExecucoes()
        );

        assertEquals(
                4,
                central.descoberta()
                        .totalEncontrados()
        );

        assertEquals(
                1,
                central.descoberta()
                        .totalImportados()
        );

        assertEquals(
                1,
                central.descoberta()
                        .totalFalhas()
        );

        assertEquals(
                "0046719-97.2011.8.26.0602",
                central.descoberta()
                        .ultimaExecucao()
                        .lotes()
                        .getFirst()
                        .numeroProcessoFormatado()
        );

        assertEquals(
                "Importado",
                central.descoberta()
                        .ultimaExecucao()
                        .lotes()
                        .getFirst()
                        .decisaoFormatada()
        );
    }

    private HistoricoAcompanhamento criarHistorico(
            Long imovelId,
            LocalDateTime data,
            String origem,
            StatusPipeline statusPipeline,
            String descricao
    ) {

        Processo processo =
                mock(
                        Processo.class
                );

        Imovel imovel =
                mock(
                        Imovel.class
                );

        Acompanhamento acompanhamento =
                mock(
                        Acompanhamento.class
                );

        HistoricoAcompanhamento historico =
                mock(
                        HistoricoAcompanhamento.class
                );


        when(processo.getNumeroProcesso())
                .thenReturn(
                        "12345678920268260602"
                );

        when(imovel.getId())
                .thenReturn(
                        imovelId
                );

        when(imovel.getTipo())
                .thenReturn(
                        "Casa"
                );

        when(imovel.getBairro())
                .thenReturn(
                        "Centro"
                );

        when(imovel.getCidade())
                .thenReturn(
                        "Sorocaba"
                );

        when(imovel.getProcesso())
                .thenReturn(
                        processo
                );

        when(acompanhamento.getImovel())
                .thenReturn(
                        imovel
                );

        when(historico.getAcompanhamento())
                .thenReturn(
                        acompanhamento
                );

        when(historico.getDataEvento())
                .thenReturn(
                        data
                );

        when(historico.getOrigem())
                .thenReturn(
                        origem
                );

        when(historico.getStatusPipeline())
                .thenReturn(
                        statusPipeline
                );

        when(historico.getStatusLeilao())
                .thenReturn(
                        StatusLeilao.ENCERRADO
                );

        when(historico.getResultadoLeilao())
                .thenReturn(
                        ResultadoLeilao.SEM_LANCES
                );

        when(historico.getDescricao())
                .thenReturn(
                        descricao
                );


        return historico;
    }
}
