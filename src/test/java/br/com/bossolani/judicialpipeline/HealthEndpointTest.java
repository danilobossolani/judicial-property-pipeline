package br.com.bossolani.judicialpipeline;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthEndpointTest {

    @LocalServerPort
    private int port;

    @Test
    void deveExporSomenteEstadoBasicoDeSaude()
            throws IOException, InterruptedException {

        HttpResponse<String> response =
                requisitar("/actuator/health");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"");
        assertThat(response.body()).doesNotContain(
                "db",
                "database",
                "diskSpace",
                "components"
        );
    }

    @Test
    void deveRenderizarPainelResponsivoEmEstadoVazio()
            throws IOException, InterruptedException {

        HttpResponse<String> response =
                requisitar("/");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("Pipeline Judicial")
                .contains("Oportunidades aprovadas")
                .contains("width=device-width");
    }

    @Test
    void deveRenderizarCentralDeAuditoriaComDescobertaMultifuente()
            throws IOException, InterruptedException {

        HttpResponse<String> response =
                requisitar("/auditoria");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("Central de auditoria")
                .contains("Descoberta automática")
                .contains("Nenhuma descoberta foi executada");
    }

    private HttpResponse<String> requisitar(
            String caminho
    ) throws IOException, InterruptedException {

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        "http://localhost:"
                                                + port
                                                + caminho
                                )
                        )
                        .GET()
                        .build();

        return HttpClient.newHttpClient()
                .send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );
    }
}
