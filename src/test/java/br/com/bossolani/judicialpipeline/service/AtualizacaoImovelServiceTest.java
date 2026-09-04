package br.com.bossolani.judicialpipeline.service;

import br.com.bossolani.judicialpipeline.model.Fonte;
import br.com.bossolani.judicialpipeline.model.FonteTipo;
import br.com.bossolani.judicialpipeline.model.Leilao;
import br.com.bossolani.judicialpipeline.repository.FonteRepository;
import br.com.bossolani.judicialpipeline.repository.LeilaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AtualizacaoImovelServiceTest {

    private LeilaoRepository leilaoRepository;
    private FonteRepository fonteRepository;
    private PersistenciaLeilaoService persistenciaLeilaoService;
    private TriagemLoteService triagemLoteService;
    private AtualizacaoImovelService service;

    @BeforeEach
    void preparar() {
        leilaoRepository = mock(LeilaoRepository.class);
        fonteRepository = mock(FonteRepository.class);
        persistenciaLeilaoService = mock(PersistenciaLeilaoService.class);
        triagemLoteService = mock(TriagemLoteService.class);
        service = new AtualizacaoImovelService(
                leilaoRepository,
                fonteRepository,
                persistenciaLeilaoService,
                triagemLoteService
        );
    }

    @Test
    void deveAtualizarFonteDeLeiloeiroIntegrado() throws Exception {
        String original = "https://www.glleiloes.com.br/item/5081/detalhes?page=1";
        String normalizada = "https://www.glleiloes.com.br/item/5081/detalhes";
        prepararFonte(original, FonteTipo.LEILOEIRO_OFICIAL);
        when(triagemLoteService.normalizarUrl(original)).thenReturn(normalizada);

        service.atualizarDados(10L);

        verify(persistenciaLeilaoService).coletarESalvar(normalizada);
    }

    @Test
    void deveAtualizarComunicacaoDoDjen() throws Exception {
        String url = "https://comunica.pje.jus.br/consulta/AbCdEfGhIjKlMnOp"
                + "?numeroProcesso=10123456720248260602";
        prepararFonte(url, FonteTipo.DJE_TJSP);
        when(triagemLoteService.normalizarUrl(url)).thenReturn(url);

        service.atualizarDados(10L);

        verify(persistenciaLeilaoService).coletarESalvar(url);
    }

    @Test
    void deveRecusarUrlQueNenhumProviderReconhece() {
        String url = "https://exemplo.invalid/lote/1";
        prepararFonte(url, FonteTipo.LEILOEIRO_OFICIAL);
        when(triagemLoteService.normalizarUrl(url))
                .thenThrow(new IllegalArgumentException("não permitida"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.atualizarDados(10L)
        );

        assertEquals(400, exception.getStatusCode().value());
    }

    private void prepararFonte(String url, FonteTipo tipo) {
        Leilao leilao = mock(Leilao.class);
        when(leilao.getId()).thenReturn(20L);
        when(leilaoRepository.findTopByImovelIdOrderByIdDesc(10L))
                .thenReturn(Optional.of(leilao));

        Fonte fonte = new Fonte();
        fonte.setTipo(tipo);
        fonte.setUrlOrigem(url);
        when(fonteRepository.findByLeilaoIdOrderByDataCapturaDesc(20L))
                .thenReturn(List.of(fonte));
    }
}
