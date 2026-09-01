package br.com.bossolani.judicialpipeline.service;

import br.com.bossolani.judicialpipeline.exception.LoteDescartadoException;
import br.com.bossolani.judicialpipeline.integration.datajud.dto.DataJudProcessoDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.DadosDinamicosLeilaoDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteEnriquecidoDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteLeilaoDTO;
import br.com.bossolani.judicialpipeline.model.Acompanhamento;
import br.com.bossolani.judicialpipeline.model.Fonte;
import br.com.bossolani.judicialpipeline.model.Imovel;
import br.com.bossolani.judicialpipeline.model.Leilao;
import br.com.bossolani.judicialpipeline.model.Processo;
import br.com.bossolani.judicialpipeline.model.StatusPipeline;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PersistenciaLeilaoServiceTest {

    @Test
    void deveDescartarClasseDeDespejoAntesDePersistir()
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


        PersistenciaLeilaoService service =
                new PersistenciaLeilaoService(
                        integracao,
                        processoRepository,
                        imovelRepository,
                        leilaoRepository,
                        fonteRepository,
                        acompanhamentoRepository,
                        historicoRepository,
                        triagemSemAlterarUrl()
                );


        String url =
                "https://www.sublimeleiloes.com.br/lote/casa-em-sorocaba/9997/";


        LoteLeilaoDTO lote =
                new LoteLeilaoDTO(
                        "0050699-62.2005.8.26.0602",
                        new BigDecimal("935563.84"),
                        "Sorocaba",
                        "5ª Vara Cível",
                        "Casa",
                        "Rua Manoel Lourenço Rodrigues, 45 - Sorocaba - SP",
                        "45",
                        "Vila Barão",
                        url
                );


        DadosDinamicosLeilaoDTO dadosLeilao =
                new DadosDinamicosLeilaoDTO(
                        LocalDateTime.of(
                                2026,
                                6,
                                29,
                                9,
                                0
                        ),
                        null,
                        null,
                        null,
                        null,
                        null,
                        50,
                        "ENCERRADO",
                        "SEM LANCES",
                        null,
                        null,
                        null
                );


        DataJudProcessoDTO dadosProcesso =
                new DataJudProcessoDTO(
                        "00506996220058260602",
                        "TJSP",
                        "G1",
                        null,
                        "05 CÍVEL DE SOROCABA",
                        "Despejo por Falta de Pagamento",
                        "SAJ",
                        "Eletrônico",
                        null
                );


        when(integracao.buscarLote(
                url
        )).thenReturn(
                new LoteEnriquecidoDTO(
                        lote,
                        dadosLeilao,
                        dadosProcesso,
                        true,
                        "Sublime Leilões"
                )
        );


        LoteDescartadoException exception =
                assertThrows(
                        LoteDescartadoException.class,
                        () -> service.coletarESalvar(
                                url
                        )
                );


        assertEquals(
                "Ação de despejo não representa oportunidade imobiliária.",
                exception.getMessage()
        );


        verify(processoRepository, never()).save(
                any(Processo.class)
        );

        verify(imovelRepository, never()).save(
                any(Imovel.class)
        );

        verify(leilaoRepository, never()).save(
                any(Leilao.class)
        );

        verify(fonteRepository, never()).save(
                any(Fonte.class)
        );
    }

    @Test
    void deveReutilizarImovelECriarLeilaoSeparadoQuandoProcessoJaExiste()
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
                triagemSemAlterarUrl();


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
                        false,
                        "Mega Leilões"
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


        when(leilaoRepository.save(
                any(Leilao.class)
        )).thenAnswer(invocacao ->
                invocacao.getArgument(0)
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
                imovelExistente,
                fonte.getLeilao().getImovel()
        );

        assertEquals(
                new BigDecimal("368609.26"),
                fonte.getLeilao().getValorAvaliacaoFonte()
        );

        assertEquals(
                "Mega Leilões",
                fonte.getOrigemNome()
        );


        verify(imovelRepository).save(
                imovelExistente
        );

        verify(leilaoRepository).save(
                any(Leilao.class)
        );


        ArgumentCaptor<Fonte> fonteCaptor =
                ArgumentCaptor.forClass(
                        Fonte.class
                );


        verify(fonteRepository).save(
                fonteCaptor.capture()
        );


        assertSame(
                fonte.getLeilao(),
                fonteCaptor.getValue().getLeilao()
        );
    }

    @Test
    void deveArquivarFonteExistenteQuandoDataJudConfirmarDespejo()
            throws Exception {

        IntegracaoLeilaoService integracao =
                mock(IntegracaoLeilaoService.class);
        ProcessoRepository processoRepository =
                mock(ProcessoRepository.class);
        ImovelRepository imovelRepository =
                mock(ImovelRepository.class);
        LeilaoRepository leilaoRepository =
                mock(LeilaoRepository.class);
        FonteRepository fonteRepository =
                mock(FonteRepository.class);
        AcompanhamentoRepository acompanhamentoRepository =
                mock(AcompanhamentoRepository.class);
        HistoricoAcompanhamentoRepository historicoRepository =
                mock(HistoricoAcompanhamentoRepository.class);

        PersistenciaLeilaoService service =
                new PersistenciaLeilaoService(
                        integracao,
                        processoRepository,
                        imovelRepository,
                        leilaoRepository,
                        fonteRepository,
                        acompanhamentoRepository,
                        historicoRepository,
                        triagemSemAlterarUrl()
                );

        String url =
                "https://www.sublimeleiloes.com.br/lote/casa-em-sorocaba/9997/";
        LoteLeilaoDTO lote =
                new LoteLeilaoDTO(
                        "0050699-62.2005.8.26.0602",
                        new BigDecimal("935563.84"),
                        "Sorocaba",
                        "5ª Vara Cível",
                        "Casa",
                        "Rua de teste",
                        "45",
                        "Centro",
                        url
                );
        DadosDinamicosLeilaoDTO dadosLeilao =
                new DadosDinamicosLeilaoDTO(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        50,
                        "ENCERRADO",
                        "SEM LANCES",
                        null,
                        null,
                        null
                );
        DataJudProcessoDTO dadosProcesso =
                new DataJudProcessoDTO(
                        "00506996220058260602",
                        "TJSP",
                        "G1",
                        null,
                        "05 CÍVEL DE SOROCABA",
                        "Despejo por Falta de Pagamento",
                        "SAJ",
                        "Eletrônico",
                        null
                );

        Processo processo = mock(Processo.class);
        Imovel imovel = mock(Imovel.class);
        when(imovel.getId()).thenReturn(20L);
        when(imovel.getProcesso()).thenReturn(processo);

        Leilao leilao = new Leilao();
        leilao.setImovel(imovel);

        Fonte fonte = new Fonte();
        fonte.setLeilao(leilao);

        Acompanhamento acompanhamento =
                new Acompanhamento();
        acompanhamento.setStatusPipeline(
                StatusPipeline.OPORTUNIDADE
        );
        acompanhamento.setAtivo(true);

        when(integracao.buscarLote(url))
                .thenReturn(
                        new LoteEnriquecidoDTO(
                                lote,
                                dadosLeilao,
                                dadosProcesso,
                                true,
                                "Sublime Leilões"
                        )
                );
        when(fonteRepository.findByUrlOrigem(url))
                .thenReturn(Optional.of(fonte));
        when(acompanhamentoRepository.findByImovelId(20L))
                .thenReturn(Optional.of(acompanhamento));

        assertThrows(
                LoteDescartadoException.class,
                () -> service.coletarESalvar(url)
        );

        assertEquals(
                StatusPipeline.DESCARTADO,
                acompanhamento.getStatusPipeline()
        );
        assertEquals(false, acompanhamento.isAtivo());
        verify(processoRepository).save(processo);
        verify(acompanhamentoRepository).save(acompanhamento);
        verify(historicoRepository).save(any());
        verify(fonteRepository).save(fonte);
    }

    private TriagemLoteService triagemSemAlterarUrl() {

        TriagemLoteService triagem =
                mock(TriagemLoteService.class);

        when(triagem.normalizarUrl(anyString()))
                .thenAnswer(invocacao ->
                        invocacao.getArgument(0)
                );

        return triagem;
    }
}
