package br.com.bossolani.judicialpipeline.service;

import br.com.bossolani.judicialpipeline.dto.ResultadoDescobertaDTO;
import br.com.bossolani.judicialpipeline.exception.DescobertaEmAndamentoException;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteDescobertoDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.sublime.SublimeLeiloesDiscoveryBrowser;
import br.com.bossolani.judicialpipeline.model.DecisaoLoteDescoberta;
import br.com.bossolani.judicialpipeline.model.ExecucaoDescoberta;
import br.com.bossolani.judicialpipeline.model.Fonte;
import br.com.bossolani.judicialpipeline.model.Imovel;
import br.com.bossolani.judicialpipeline.model.Leilao;
import br.com.bossolani.judicialpipeline.model.Processo;
import br.com.bossolani.judicialpipeline.model.ResultadoLoteDescoberta;
import br.com.bossolani.judicialpipeline.model.StatusExecucaoDescoberta;
import br.com.bossolani.judicialpipeline.repository.ExecucaoDescobertaRepository;
import br.com.bossolani.judicialpipeline.repository.FonteRepository;
import br.com.bossolani.judicialpipeline.repository.ResultadoLoteDescobertaRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DescobertaAutomaticaServiceTest {

    @Test
    void devePersistirContadoresMotivosEContinuarAposFalhaParcial()
            throws Exception {

        Dependencias dependencias =
                novasDependencias();


        String urlNova =
                "https://www.sublimeleiloes.com.br/lote/casa-em-sorocaba/2405/";

        String urlDuplicada =
                "https://www.sublimeleiloes.com.br/lote/area-em-sorocaba/2201/";

        String urlVeiculo =
                "https://www.sublimeleiloes.com.br/lote/moto-em-sorocaba/9999/";

        String urlComFalha =
                "https://www.sublimeleiloes.com.br/lote/galpao-em-votorantim/9998/";


        when(dependencias.browser().descobrirLotes())
                .thenReturn(
                        List.of(
                                lote(
                                        urlNova,
                                        "Casa em Sorocaba",
                                        "Sorocaba",
                                        "Residenciais"
                                ),
                                lote(
                                        urlDuplicada,
                                        "Área em Sorocaba",
                                        "Sorocaba",
                                        "Terrenos"
                                ),
                                lote(
                                        urlVeiculo,
                                        "Moto em Sorocaba",
                                        "Sorocaba",
                                        "Veículos Motos"
                                ),
                                lote(
                                        urlComFalha,
                                        "Galpão em Votorantim",
                                        "Votorantim",
                                        "Imóvel comercial"
                                )
                        )
                );


        when(dependencias.fonteRepository().findByUrlOrigem(
                urlDuplicada
        )).thenReturn(
                Optional.of(
                        new Fonte()
                )
        );


        when(dependencias.persistencia().coletarESalvar(
                urlNova
        )).thenReturn(
                new Fonte()
        );

        when(dependencias.persistencia().coletarESalvar(
                urlComFalha
        )).thenThrow(
                new IllegalStateException(
                        "página do lote indisponível"
                )
        );


        ResultadoDescobertaDTO resultado =
                dependencias.service()
                        .executarDescoberta();


        assertEquals(
                4,
                resultado.encontrados()
        );

        assertEquals(
                3,
                resultado.elegiveis()
        );

        assertEquals(
                1,
                resultado.importados()
        );

        assertEquals(
                1,
                resultado.duplicados()
        );

        assertEquals(
                1,
                resultado.descartados()
        );

        assertEquals(
                1,
                resultado.falhas()
        );

        assertEquals(
                StatusExecucaoDescoberta.CONCLUIDA_COM_FALHAS,
                resultado.status()
        );

        assertNotNull(
                resultado.termino()
        );

        assertNotNull(
                resultado.duracaoMs()
        );

        assertTrue(
                resultado.erroResumo()
                        .contains("página do lote indisponível")
        );


        ArgumentCaptor<ResultadoLoteDescoberta> captor =
                ArgumentCaptor.forClass(
                        ResultadoLoteDescoberta.class
                );


        verify(dependencias.resultadoRepository(), times(4)).save(
                captor.capture()
        );


        List<ResultadoLoteDescoberta> lotesPersistidos =
                captor.getAllValues();


        assertEquals(
                List.of(
                        DecisaoLoteDescoberta.IMPORTADO,
                        DecisaoLoteDescoberta.DUPLICADO,
                        DecisaoLoteDescoberta.DESCARTADO,
                        DecisaoLoteDescoberta.FALHA
                ),
                lotesPersistidos.stream()
                        .map(ResultadoLoteDescoberta::getDecisao)
                        .toList()
        );

        assertEquals(
                "URL normalizada já cadastrada como fonte do pipeline.",
                lotesPersistidos.get(1)
                        .getMotivo()
        );

        assertEquals(
                "Bem móvel identificado na descrição do lote.",
                lotesPersistidos.get(2)
                        .getMotivo()
        );

        assertTrue(
                lotesPersistidos.get(3)
                        .getMotivo()
                        .contains("página do lote indisponível")
        );


        verify(dependencias.persistencia()).coletarESalvar(
                urlNova
        );

        verify(dependencias.persistencia(), never()).coletarESalvar(
                urlDuplicada
        );

        verify(dependencias.persistencia(), never()).coletarESalvar(
                urlVeiculo
        );

        verify(dependencias.persistencia()).coletarESalvar(
                urlComFalha
        );
    }

    @Test
    void deveClassificarProcessoExistenteComoDuplicadoEPreservarFonte()
            throws Exception {

        Dependencias dependencias =
                novasDependencias();

        String url =
                "https://www.sublimeleiloes.com.br/lote/casa-em-sorocaba/2405/";


        when(dependencias.browser().descobrirLotes())
                .thenReturn(
                        List.of(
                                lote(
                                        url,
                                        "Casa em Sorocaba",
                                        "Sorocaba",
                                        "Residenciais"
                                )
                        )
                );


        Fonte fonte =
                mock(
                        Fonte.class
                );

        Leilao leilao =
                mock(
                        Leilao.class
                );

        Imovel imovel =
                mock(
                        Imovel.class
                );

        Processo processo =
                mock(
                        Processo.class
                );


        when(fonte.getLeilao())
                .thenReturn(
                        leilao
                );

        when(leilao.getImovel())
                .thenReturn(
                        imovel
                );

        when(imovel.getId())
                .thenReturn(
                        25L
                );

        when(imovel.getProcesso())
                .thenReturn(
                        processo
                );

        when(processo.getNumeroProcesso())
                .thenReturn(
                        "00467199720118260602"
                );

        when(dependencias.persistencia().coletarESalvar(
                url
        )).thenReturn(
                fonte
        );

        when(dependencias.fonteRepository().countByLeilaoImovelId(
                25L
        )).thenReturn(
                2L
        );


        ResultadoDescobertaDTO resultado =
                dependencias.service()
                        .executarDescoberta();


        assertEquals(
                0,
                resultado.importados()
        );

        assertEquals(
                1,
                resultado.duplicados()
        );


        ArgumentCaptor<ResultadoLoteDescoberta> captor =
                ArgumentCaptor.forClass(
                        ResultadoLoteDescoberta.class
                );


        verify(dependencias.resultadoRepository()).save(
                captor.capture()
        );


        assertEquals(
                DecisaoLoteDescoberta.DUPLICADO,
                captor.getValue()
                        .getDecisao()
        );

        assertEquals(
                "00467199720118260602",
                captor.getValue()
                        .getNumeroProcesso()
        );

        assertEquals(
                imovel,
                captor.getValue()
                        .getImovel()
        );

        assertTrue(
                captor.getValue()
                        .getMotivo()
                        .contains("nova fonte foi preservada")
        );
    }

    @Test
    void deveBloquearExecucoesSimultaneas()
            throws Exception {

        Dependencias dependencias =
                novasDependencias();

        CountDownLatch browserIniciado =
                new CountDownLatch(1);

        CountDownLatch liberarBrowser =
                new CountDownLatch(1);


        when(dependencias.browser().descobrirLotes())
                .thenAnswer(invocacao -> {

                    browserIniciado.countDown();

                    liberarBrowser.await(
                            5,
                            TimeUnit.SECONDS
                    );


                    return List.of();
                });


        ExecutorService executor =
                Executors.newSingleThreadExecutor();


        try {

            Future<ResultadoDescobertaDTO> primeira =
                    executor.submit(
                            () -> {
                                return dependencias.service()
                                        .executarDescoberta();
                            }
                    );


            assertTrue(
                    browserIniciado.await(
                            5,
                            TimeUnit.SECONDS
                    )
            );


            assertThrows(
                    DescobertaEmAndamentoException.class,
                    dependencias.service()::executarDescoberta
            );


            liberarBrowser.countDown();


            assertEquals(
                    StatusExecucaoDescoberta.CONCLUIDA,
                    primeira.get(
                            5,
                            TimeUnit.SECONDS
                    ).status()
            );

        } finally {

            liberarBrowser.countDown();
            executor.shutdownNow();
        }
    }

    private Dependencias novasDependencias() {

        SublimeLeiloesDiscoveryBrowser browser =
                mock(
                        SublimeLeiloesDiscoveryBrowser.class
                );

        FonteRepository fonteRepository =
                mock(
                        FonteRepository.class
                );

        PersistenciaLeilaoService persistencia =
                mock(
                        PersistenciaLeilaoService.class
                );

        ExecucaoDescobertaRepository execucaoRepository =
                mock(
                        ExecucaoDescobertaRepository.class
                );

        ResultadoLoteDescobertaRepository resultadoRepository =
                mock(
                        ResultadoLoteDescobertaRepository.class
                );


        when(fonteRepository.findByUrlOrigem(
                anyString()
        )).thenReturn(
                Optional.empty()
        );

        when(execucaoRepository.save(
                any(ExecucaoDescoberta.class)
        )).thenAnswer(invocacao ->
                invocacao.getArgument(0)
        );

        when(resultadoRepository.save(
                any(ResultadoLoteDescoberta.class)
        )).thenAnswer(invocacao ->
                invocacao.getArgument(0)
        );


        DescobertaAutomaticaService service =
                new DescobertaAutomaticaService(
                        browser,
                        new TriagemLoteService(),
                        fonteRepository,
                        persistencia,
                        execucaoRepository,
                        resultadoRepository
                );


        return new Dependencias(
                service,
                browser,
                fonteRepository,
                persistencia,
                execucaoRepository,
                resultadoRepository
        );
    }

    private LoteDescobertoDTO lote(
            String url,
            String titulo,
            String cidade,
            String resumo
    ) {

        return new LoteDescobertoDTO(
                url,
                titulo,
                cidade,
                resumo
        );
    }

    private record Dependencias(
            DescobertaAutomaticaService service,
            SublimeLeiloesDiscoveryBrowser browser,
            FonteRepository fonteRepository,
            PersistenciaLeilaoService persistencia,
            ExecucaoDescobertaRepository execucaoRepository,
            ResultadoLoteDescobertaRepository resultadoRepository
    ) {
    }
}
