package br.com.bossolani.judicialpipeline.service;

import br.com.bossolani.judicialpipeline.model.Acompanhamento;
import br.com.bossolani.judicialpipeline.model.HistoricoAcompanhamento;
import br.com.bossolani.judicialpipeline.model.Imovel;
import br.com.bossolani.judicialpipeline.model.Leilao;
import br.com.bossolani.judicialpipeline.model.ResultadoLeilao;
import br.com.bossolani.judicialpipeline.model.StatusLeilao;
import br.com.bossolani.judicialpipeline.model.StatusPipeline;
import br.com.bossolani.judicialpipeline.repository.AcompanhamentoRepository;
import br.com.bossolani.judicialpipeline.repository.HistoricoAcompanhamentoRepository;
import br.com.bossolani.judicialpipeline.repository.LeilaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CicloVidaLeilaoServiceTest {

    private static final LocalDateTime AGORA =
            LocalDateTime.of(2026, 9, 3, 12, 0);

    private AcompanhamentoRepository acompanhamentoRepository;
    private LeilaoRepository leilaoRepository;
    private HistoricoAcompanhamentoRepository historicoRepository;
    private CicloVidaLeilaoService service;

    @BeforeEach
    void preparar() {

        acompanhamentoRepository = mock(AcompanhamentoRepository.class);
        leilaoRepository = mock(LeilaoRepository.class);
        historicoRepository = mock(HistoricoAcompanhamentoRepository.class);

        Clock relogio = Clock.fixed(
                Instant.parse("2026-09-03T15:00:00Z"),
                ZoneId.of("America/Sao_Paulo")
        );

        service = new CicloVidaLeilaoService(
                acompanhamentoRepository,
                leilaoRepository,
                historicoRepository,
                relogio
        );
    }

    @Test
    void deveArquivarImovelQuandoTodasAsFontesJaVenceram() {

        Imovel imovel = mock(Imovel.class);
        when(imovel.getId()).thenReturn(10L);

        Acompanhamento acompanhamento = acompanhamento(
                imovel,
                StatusPipeline.MONITORANDO_LEILAO
        );
        Leilao antigo = leilao(
                AGORA.minusDays(2),
                ResultadoLeilao.SEM_LANCES
        );
        Leilao recente = leilao(
                AGORA.minusMinutes(1),
                ResultadoLeilao.DESCONHECIDO
        );

        when(acompanhamentoRepository.findByAtivoTrue())
                .thenReturn(List.of(acompanhamento));
        when(leilaoRepository.findByImovelIdOrderByIdDesc(10L))
                .thenReturn(List.of(recente, antigo));

        int arquivados = service.arquivarLeiloesVencidos();

        assertThat(arquivados).isEqualTo(1);
        assertThat(acompanhamento.isAtivo()).isFalse();
        assertThat(acompanhamento.getStatusPipeline())
                .isEqualTo(StatusPipeline.AGUARDANDO_RESULTADO);
        assertThat(antigo.getStatusLeilao())
                .isEqualTo(StatusLeilao.ENCERRADO);
        assertThat(recente.getStatusLeilao())
                .isEqualTo(StatusLeilao.ENCERRADO);

        ArgumentCaptor<HistoricoAcompanhamento> captor =
                ArgumentCaptor.forClass(HistoricoAcompanhamento.class);
        verify(historicoRepository).save(captor.capture());
        assertThat(captor.getValue().getOrigem())
                .isEqualTo("Automático - prazo do leilão");
        assertThat(captor.getValue().getDescricao())
                .contains("saiu da tela inicial")
                .contains("03/09/2026 às 11:59");
    }

    @Test
    void naoDeveArquivarEnquantoQualquerFonteTiverPrazoFuturo() {

        Imovel imovel = mock(Imovel.class);
        when(imovel.getId()).thenReturn(20L);

        Acompanhamento acompanhamento = acompanhamento(
                imovel,
                StatusPipeline.MONITORANDO_LEILAO
        );
        Leilao vencido = leilao(
                AGORA.minusDays(1),
                ResultadoLeilao.DESCONHECIDO
        );
        Leilao futuro = leilao(
                AGORA.plusDays(1),
                ResultadoLeilao.DESCONHECIDO
        );

        when(acompanhamentoRepository.findByAtivoTrue())
                .thenReturn(List.of(acompanhamento));
        when(leilaoRepository.findByImovelIdOrderByIdDesc(20L))
                .thenReturn(List.of(futuro, vencido));

        int arquivados = service.arquivarLeiloesVencidos();

        assertThat(arquivados).isZero();
        assertThat(acompanhamento.isAtivo()).isTrue();
        assertThat(acompanhamento.getStatusPipeline())
                .isEqualTo(StatusPipeline.MONITORANDO_LEILAO);
        verify(acompanhamentoRepository, never()).save(acompanhamento);
        verify(historicoRepository, never()).save(
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void devePreservarDecisaoHumanaDeOportunidadeAoArquivar() {

        Imovel imovel = mock(Imovel.class);
        when(imovel.getId()).thenReturn(30L);

        Acompanhamento acompanhamento = acompanhamento(
                imovel,
                StatusPipeline.OPORTUNIDADE
        );

        when(acompanhamentoRepository.findByAtivoTrue())
                .thenReturn(List.of(acompanhamento));
        when(leilaoRepository.findByImovelIdOrderByIdDesc(30L))
                .thenReturn(List.of(leilao(
                        AGORA.minusHours(1),
                        ResultadoLeilao.DESCONHECIDO
                )));

        service.arquivarLeiloesVencidos();

        assertThat(acompanhamento.isAtivo()).isFalse();
        assertThat(acompanhamento.getStatusPipeline())
                .isEqualTo(StatusPipeline.OPORTUNIDADE);
    }

    private Acompanhamento acompanhamento(
            Imovel imovel,
            StatusPipeline status
    ) {

        Acompanhamento acompanhamento = new Acompanhamento();
        acompanhamento.setImovel(imovel);
        acompanhamento.setStatusPipeline(status);
        acompanhamento.setAtivo(true);
        acompanhamento.setDataIdentificacao(AGORA.minusDays(10));
        acompanhamento.setUltimaVerificacao(AGORA.minusHours(1));
        return acompanhamento;
    }

    private Leilao leilao(
            LocalDateTime fechamento,
            ResultadoLeilao resultado
    ) {

        Leilao leilao = new Leilao();
        leilao.setFechamento2Praca(fechamento);
        leilao.setStatusLeilao(StatusLeilao.AGENDADO);
        leilao.setResultadoLeilao(resultado);
        return leilao;
    }
}
