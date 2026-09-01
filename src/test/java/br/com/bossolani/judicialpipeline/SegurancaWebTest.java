package br.com.bossolani.judicialpipeline;

import br.com.bossolani.judicialpipeline.dto.ResultadoDescobertaDTO;
import br.com.bossolani.judicialpipeline.model.OrigemExecucaoDescoberta;
import br.com.bossolani.judicialpipeline.model.StatusExecucaoDescoberta;
import br.com.bossolani.judicialpipeline.service.ObservabilidadePipelineService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.security.enabled=true",
                "app.security.username=operador-teste",
                "app.security.password=senha-segura-de-teste",
                "management.endpoints.web.exposure.include=health,prometheus"
        }
)
class SegurancaWebTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObservabilidadePipelineService observabilidade;

    @Test
    void deveManterSaudePublicaERedirecionarInterfaceParaLogin()
            throws IOException, InterruptedException {

        HttpResponse<String> health =
                requisitar("/actuator/health", null);

        HttpResponse<String> painel =
                requisitar("/", null);

        HttpResponse<String> login =
                requisitar("/login", null);

        assertThat(health.statusCode()).isEqualTo(200);
        assertThat(health.body()).contains("\"status\":\"UP\"");
        assertThat(health.body()).doesNotContain("components");

        assertThat(painel.statusCode()).isEqualTo(302);
        assertThat(painel.headers().firstValue("location"))
                .hasValueSatisfying(location ->
                        assertThat(location).contains("/login")
                );

        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(login.body())
                .contains("Acesso protegido")
                .contains("Pipeline Judicial")
                .contains("width=device-width");
    }

    @Test
    void deveProtegerPrometheusEExporMetricasAgregadasAoOperador()
            throws IOException, InterruptedException {

        observabilidade.registrarDescoberta(
                new ResultadoDescobertaDTO(
                        1L,
                        "Sublime Leilões + Mega Leilões",
                        LocalDateTime.now().minusSeconds(1),
                        LocalDateTime.now(),
                        1000L,
                        OrigemExecucaoDescoberta.AGENDADA,
                        StatusExecucaoDescoberta.CONCLUIDA,
                        4,
                        3,
                        1,
                        2,
                        0,
                        0,
                        null
                )
        );

        HttpResponse<String> semCredencial =
                requisitar("/actuator/prometheus", null);

        HttpResponse<String> autenticado =
                requisitar(
                        "/actuator/prometheus",
                        cabecalhoBasic(
                                "operador-teste",
                                "senha-segura-de-teste"
                        )
                );

        assertThat(semCredencial.statusCode()).isEqualTo(401);
        assertThat(autenticado.statusCode()).isEqualTo(200);
        assertThat(autenticado.body())
                .contains(
                        "judicial_pipeline_descoberta_execucoes_total"
                )
                .contains(
                        "judicial_pipeline_descoberta_lotes_total"
                )
                .doesNotContain(
                        "Sublime Leilões",
                        "Mega Leilões"
                );
    }

    private HttpResponse<String> requisitar(
            String caminho,
            String authorization
    ) throws IOException, InterruptedException {

        HttpRequest.Builder request =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        "http://localhost:"
                                                + port
                                                + caminho
                                )
                        )
                        .GET();

        if (authorization != null) {
            request.header("Authorization", authorization);
        }

        return HttpClient.newBuilder()
                .followRedirects(
                        HttpClient.Redirect.NEVER
                )
                .build()
                .send(
                        request.build(),
                        HttpResponse.BodyHandlers.ofString()
                );
    }

    private String cabecalhoBasic(
            String usuario,
            String senha
    ) {

        String credencial = usuario + ":" + senha;

        return "Basic "
                + Base64.getEncoder().encodeToString(
                credencial.getBytes(StandardCharsets.UTF_8)
        );
    }
}
