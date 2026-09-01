package br.com.bossolani.judicialpipeline.service;

import br.com.bossolani.judicialpipeline.dto.ResultadoDescobertaDTO;
import br.com.bossolani.judicialpipeline.model.OrigemExecucaoDescoberta;
import br.com.bossolani.judicialpipeline.model.StatusExecucaoDescoberta;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ObservabilidadePipelineServiceTest {

    @Test
    void deveRegistrarSomenteContadoresAgregadosSemDadosSensiveis() {

        SimpleMeterRegistry registry =
                new SimpleMeterRegistry();

        ObservabilidadePipelineService service =
                new ObservabilidadePipelineService(registry);

        ResultadoDescobertaDTO resultado =
                new ResultadoDescobertaDTO(
                        99L,
                        "Fonte que não pode virar tag",
                        LocalDateTime.now().minusSeconds(2),
                        LocalDateTime.now(),
                        1500L,
                        OrigemExecucaoDescoberta.MANUAL,
                        StatusExecucaoDescoberta.CONCLUIDA_COM_FALHAS,
                        7,
                        5,
                        2,
                        2,
                        1,
                        1,
                        "processo e URL não podem virar tag"
                );

        service.registrarDescoberta(resultado);

        assertThat(
                registry.get(
                                "judicial.pipeline.descoberta.execucoes"
                        )
                        .tag("origem", "MANUAL")
                        .tag(
                                "status",
                                "CONCLUIDA_COM_FALHAS"
                        )
                        .counter()
                        .count()
        ).isEqualTo(1);

        assertThat(
                registry.get(
                                "judicial.pipeline.descoberta.lotes"
                        )
                        .tag("resultado", "importado")
                        .counter()
                        .count()
        ).isEqualTo(2);

        assertThat(
                registry.get(
                                "judicial.pipeline.descoberta.duracao"
                        )
                        .tag(
                                "status",
                                "CONCLUIDA_COM_FALHAS"
                        )
                        .timer()
                        .totalTime(
                                java.util.concurrent.TimeUnit.MILLISECONDS
                        )
        ).isEqualTo(1500);

        assertThat(registry.getMeters())
                .allSatisfy(meter -> {
                    assertThat(meter.getId().getTags())
                            .noneMatch(tag ->
                                    tag.getValue().contains("Fonte")
                                            || tag.getValue().contains("processo")
                                            || tag.getValue().contains("URL")
                            );
                });
    }
}
