package br.com.bossolani.judicialpipeline.service;

import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.DadosDinamicosLeilaoDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteEnriquecidoDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteLeilaoDTO;
import br.com.bossolani.judicialpipeline.model.Acompanhamento;
import br.com.bossolani.judicialpipeline.model.Fonte;
import br.com.bossolani.judicialpipeline.model.Imovel;
import br.com.bossolani.judicialpipeline.model.Leilao;
import br.com.bossolani.judicialpipeline.model.Processo;
import br.com.bossolani.judicialpipeline.repository.AcompanhamentoRepository;
import br.com.bossolani.judicialpipeline.repository.FonteRepository;
import br.com.bossolani.judicialpipeline.repository.HistoricoAcompanhamentoRepository;
import br.com.bossolani.judicialpipeline.repository.ImovelRepository;
import br.com.bossolani.judicialpipeline.repository.LeilaoRepository;
import br.com.bossolani.judicialpipeline.repository.ProcessoRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PersistenciaLeilaoServiceTest {

    @Test
    void deveReutilizarImovelELeilaoQuandoProcessoJaExiste()
            throws Exception {

        IntegracaoLeilaoService integracao =
                mock(
                        IntegracaoLeilaoService.class
                );

        ProcessoRepository processoRepository =
                mock(
                        ProcessoRepository.class
                );

        ImovelRepository imovelRepository =
                mock(
                        ImovelRepository.class
                );

        LeilaoRepository leilaoRepository =
                mock(
                        LeilaoRepository.class
                );

        FonteRepository fonteRepository =
                mock(
                        FonteRepository.class
                );

        AcompanhamentoRepository acompanhamentoRepository =
                mock(
                        AcompanhamentoRepository.class
                );

        HistoricoAcompanhamentoRepository historicoRepository =
                mock(
                        HistoricoAcompanhamentoRepository.class
                );


        TriagemLoteService triagem =
                new TriagemLoteService();


        PersistenciaLeilaoService service =
                new PersistenciaLeilaoService(
                        integracao,
                        processoRepository,
                        imovelRepository,
                        leilaoRepository,
                        fonteRepository,
                        acompanhamentoRepository,
                        historicoRepository,
                        triagem
                );


        String url =
                "https://www.sublimeleiloes.com.br/lote/casa-em-sorocaba/2405/";

        String numeroProcesso =
                "00467199720118260602";


        LoteLeilaoDTO lote =
                new LoteLeilaoDTO(
                        "0046719-97.2011.8.26.0602",
                        new BigDecimal("368609.26"),
                        "Sorocaba",
                        "5ª Vara Cível",
                        "Casa",
                        "Rua Pedro Pegoretti, 1210 - Sorocaba - SP",
                        "1210",
                        "Vila Barão",
                        url
                );


        DadosDinamicosLeilaoDTO dadosLeilao =
                new DadosDinamicosLeilaoDTO(
                        LocalDateTime.of(
                                2026,
                                8,
                                31,
                                9,
                                0
                        ),
                        null,
                        null,
                        null,
                        null,
                        null,
                        50,
                        "AGUARDANDO INÍCIO",
                        null,
                        null,
                        null,
                        null
                );


        when(integracao.buscarLote(
                url
        )).thenReturn(
                new LoteEnriquecidoDTO(
                        lote,
                        dadosLeilao,
                        null,
                        false
                )
        );


        Processo processoExistente =
                mock(
                        Processo.class
                );


        when(processoExistente.getId())
                .thenReturn(
                        10L
                );


        when(processoRepository.findByNumeroProcesso(
                numeroProcesso
        )).thenReturn(
                Optional.of(
                        processoExistente
                )
        );


        when(processoRepository.save(
                processoExistente
        )).thenReturn(
                processoExistente
        );


        Imovel imovelExistente =
                mock(
                        Imovel.class
                );


        when(imovelExistente.getId())
                .thenReturn(
                        20L
                );


        when(imovelRepository.findFirstByProcessoIdOrderByIdAsc(
                10L
        )).thenReturn(
                Optional.of(
                        imovelExistente
                )
        );


        when(imovelRepository.save(
                imovelExistente
        )).thenReturn(
                imovelExistente
        );


        Leilao leilaoExistente =
                new Leilao();


        when(leilaoRepository.findTopByImovelIdOrderByIdDesc(
                20L
        )).thenReturn(
                Optional.of(
                        leilaoExistente
                )
        );


        when(leilaoRepository.save(
                leilaoExistente
        )).thenReturn(
                leilaoExistente
        );


        Acompanhamento acompanhamento =
                new Acompanhamento();


        when(acompanhamentoRepository.findByImovelId(
                20L
        )).thenReturn(
                Optional.of(
                        acompanhamento
                )
        );


        when(acompanhamentoRepository.save(
                acompanhamento
        )).thenReturn(
                acompanhamento
        );


        when(fonteRepository.save(
                any(Fonte.class)
        )).thenAnswer(invocacao ->
                invocacao.getArgument(0)
        );


        Fonte fonte =
                service.coletarESalvar(
                        url
                );


        assertSame(
                leilaoExistente,
                fonte.getLeilao()
        );


        verify(imovelRepository).save(
                imovelExistente
        );

        verify(leilaoRepository).save(
                leilaoExistente
        );


        ArgumentCaptor<Fonte> fonteCaptor =
                ArgumentCaptor.forClass(
                        Fonte.class
                );


        verify(fonteRepository).save(
                fonteCaptor.capture()
        );


        assertSame(
                leilaoExistente,
                fonteCaptor.getValue()
                        .getLeilao()
        );
    }
}
