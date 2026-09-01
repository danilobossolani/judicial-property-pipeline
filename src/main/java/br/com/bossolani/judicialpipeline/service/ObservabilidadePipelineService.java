package br.com.bossolani.judicialpipeline.service;

import br.com.bossolani.judicialpipeline.dto.ResultadoDescobertaDTO;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class ObservabilidadePipelineService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    ObservabilidadePipelineService.class
            );

    private static final String METRICA_EXECUCOES =
            "judicial.pipeline.descoberta.execucoes";

    private static final String METRICA_LOTES =
            "judicial.pipeline.descoberta.lotes";

    private static final String METRICA_DURACAO =
            "judicial.pipeline.descoberta.duracao";

    private final MeterRegistry meterRegistry;

    public ObservabilidadePipelineService(
            MeterRegistry meterRegistry
    ) {
        this.meterRegistry = meterRegistry;
    }

    public void registrarDescoberta(
            ResultadoDescobertaDTO resultado
    ) {

        if (resultado == null) {
            return;
        }

        try {
            String origem =
                    resultado.origem() != null
                            ? resultado.origem().name()
                            : "DESCONHECIDA";

            String status =
                    resultado.status() != null
                            ? resultado.status().name()
                            : "DESCONHECIDO";

            meterRegistry.counter(
                    METRICA_EXECUCOES,
                    "origem", origem,
                    "status", status
            ).increment();

            registrarLotes(
                    "encontrado",
                    resultado.encontrados()
            );
            registrarLotes(
                    "elegivel",
                    resultado.elegiveis()
            );
            registrarLotes(
                    "importado",
                    resultado.importados()
            );
            registrarLotes(
                    "duplicado",
                    resultado.duplicados()
            );
            registrarLotes(
                    "descartado",
                    resultado.descartados()
            );
            registrarLotes(
                    "falha",
                    resultado.falhas()
            );

            if (resultado.duracaoMs() != null) {
                Timer.builder(METRICA_DURACAO)
                        .description(
                                "Duração das execuções de descoberta"
                        )
                        .tag("status", status)
                        .register(meterRegistry)
                        .record(
                                resultado.duracaoMs(),
                                TimeUnit.MILLISECONDS
                        );
            }

        } catch (RuntimeException exception) {
            log.warn(
                    "Não foi possível registrar métricas da execução de descoberta.",
                    exception
            );
        }
    }

    private void registrarLotes(
            String resultado,
            int quantidade
    ) {

        if (quantidade <= 0) {
            return;
        }

        meterRegistry.counter(
                METRICA_LOTES,
                "resultado", resultado
        ).increment(quantidade);
    }
}
