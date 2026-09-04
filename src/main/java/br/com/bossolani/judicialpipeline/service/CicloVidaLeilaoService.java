package br.com.bossolani.judicialpipeline.service;

import br.com.bossolani.judicialpipeline.model.Acompanhamento;
import br.com.bossolani.judicialpipeline.model.HistoricoAcompanhamento;
import br.com.bossolani.judicialpipeline.model.Leilao;
import br.com.bossolani.judicialpipeline.model.ResultadoLeilao;
import br.com.bossolani.judicialpipeline.model.StatusLeilao;
import br.com.bossolani.judicialpipeline.model.StatusPipeline;
import br.com.bossolani.judicialpipeline.repository.AcompanhamentoRepository;
import br.com.bossolani.judicialpipeline.repository.HistoricoAcompanhamentoRepository;
import br.com.bossolani.judicialpipeline.repository.LeilaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class CicloVidaLeilaoService {

    private static final Logger log =
            LoggerFactory.getLogger(CicloVidaLeilaoService.class);

    private static final DateTimeFormatter FORMATADOR_DATA =
            DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy 'às' HH:mm",
                    Locale.forLanguageTag("pt-BR")
            );

    private final AcompanhamentoRepository acompanhamentoRepository;
    private final LeilaoRepository leilaoRepository;
    private final HistoricoAcompanhamentoRepository historicoRepository;
    private final Clock clock;

    @Autowired
    public CicloVidaLeilaoService(
            AcompanhamentoRepository acompanhamentoRepository,
            LeilaoRepository leilaoRepository,
            HistoricoAcompanhamentoRepository historicoRepository
    ) {

        this(
                acompanhamentoRepository,
                leilaoRepository,
                historicoRepository,
                Clock.systemDefaultZone()
        );
    }

    CicloVidaLeilaoService(
            AcompanhamentoRepository acompanhamentoRepository,
            LeilaoRepository leilaoRepository,
            HistoricoAcompanhamentoRepository historicoRepository,
            Clock clock
    ) {

        this.acompanhamentoRepository = acompanhamentoRepository;
        this.leilaoRepository = leilaoRepository;
        this.historicoRepository = historicoRepository;
        this.clock = clock;
    }

    @Scheduled(
            initialDelayString = "${ciclo-vida.atraso-inicial-ms:10000}",
            fixedDelayString = "${ciclo-vida.intervalo-ms:60000}"
    )
    public void executarAgendamento() {

        int arquivados = arquivarLeiloesVencidos();

        if (arquivados > 0) {
            log.info(
                    "Ciclo de vida retirou {} imóvel(is) com leilão vencido da tela inicial.",
                    arquivados
            );
        }
    }

    @Transactional
    public int arquivarLeiloesVencidos() {

        LocalDateTime agora = LocalDateTime.now(clock);
        int totalArquivados = 0;

        for (Acompanhamento acompanhamento
                : acompanhamentoRepository.findByAtivoTrue()) {
            if (acompanhamento.getImovel() == null
                    || acompanhamento.getImovel().getId() == null) {
                continue;
            }

            List<Leilao> leiloes =
                    leilaoRepository.findByImovelIdOrderByIdDesc(
                            acompanhamento.getImovel().getId()
                    );

            LocalDateTime prazoFinal =
                    encontrarPrazoFinalMaisDistante(leiloes);

            if (prazoFinal == null
                    || prazoFinal.isAfter(agora)) {
                continue;
            }

            ResultadoLeilao resultado =
                    encontrarResultadoMaisRecente(leiloes);

            encerrarRetratosVencidos(leiloes, agora);

            StatusPipeline statusFinal =
                    definirStatusFinal(
                            acompanhamento.getStatusPipeline(),
                            resultado
                    );

            acompanhamento.setStatusPipeline(statusFinal);
            acompanhamento.setAtivo(false);
            acompanhamento.setUltimaVerificacao(agora);
            acompanhamentoRepository.save(acompanhamento);

            registrarHistorico(
                    acompanhamento,
                    statusFinal,
                    resultado,
                    prazoFinal,
                    agora
            );

            totalArquivados++;
        }

        return totalArquivados;
    }

    private LocalDateTime encontrarPrazoFinalMaisDistante(
            List<Leilao> leiloes
    ) {

        return leiloes.stream()
                .map(this::prazoFinal)
                .filter(data -> data != null)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    private LocalDateTime prazoFinal(
            Leilao leilao
    ) {

        if (leilao.getFechamento2Praca() != null) {
            return leilao.getFechamento2Praca();
        }

        return leilao.getFechamento1Praca();
    }

    private ResultadoLeilao encontrarResultadoMaisRecente(
            List<Leilao> leiloes
    ) {

        return leiloes.stream()
                .map(Leilao::getResultadoLeilao)
                .filter(resultado ->
                        resultado != null
                                && resultado != ResultadoLeilao.DESCONHECIDO
                )
                .findFirst()
                .orElse(ResultadoLeilao.DESCONHECIDO);
    }

    private void encerrarRetratosVencidos(
            List<Leilao> leiloes,
            LocalDateTime agora
    ) {

        boolean alterou = false;

        for (Leilao leilao : leiloes) {
            LocalDateTime prazo = prazoFinal(leilao);

            if (prazo != null
                    && !prazo.isAfter(agora)
                    && leilao.getStatusLeilao()
                    != StatusLeilao.ENCERRADO) {
                leilao.setStatusLeilao(StatusLeilao.ENCERRADO);
                alterou = true;
            }
        }

        if (alterou) {
            leilaoRepository.saveAll(leiloes);
        }
    }

    private StatusPipeline definirStatusFinal(
            StatusPipeline statusAtual,
            ResultadoLeilao resultado
    ) {

        if (statusAtual == StatusPipeline.EM_ANALISE
                || statusAtual == StatusPipeline.OPORTUNIDADE
                || statusAtual == StatusPipeline.DESCARTADO
                || statusAtual == StatusPipeline.ENCERRADO) {
            return statusAtual;
        }

        if (resultado == ResultadoLeilao.ARREMATADO) {
            return StatusPipeline.ENCERRADO;
        }

        return StatusPipeline.AGUARDANDO_RESULTADO;
    }

    private void registrarHistorico(
            Acompanhamento acompanhamento,
            StatusPipeline statusFinal,
            ResultadoLeilao resultado,
            LocalDateTime prazoFinal,
            LocalDateTime agora
    ) {

        HistoricoAcompanhamento historico =
                new HistoricoAcompanhamento();

        historico.setAcompanhamento(acompanhamento);
        historico.setDataEvento(agora);
        historico.setStatusPipeline(statusFinal);
        historico.setStatusLeilao(StatusLeilao.ENCERRADO);
        historico.setResultadoLeilao(resultado);
        historico.setOrigem("Automático - prazo do leilão");
        historico.setDescricao(
                "O prazo mais recente informado pelas fontes terminou em "
                        + prazoFinal.format(FORMATADOR_DATA)
                        + ". O imóvel saiu da tela inicial e permanece preservado nos inativos e na auditoria."
        );

        historicoRepository.save(historico);
    }
}
