package br.com.bossolani.judicialpipeline.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class DataJudClient {

    private final RestClient restClient;
    private final String apiKey;

    public DataJudClient(
            @Value("${datajud.url}") String url,
            @Value("${datajud.api-key}") String apiKey
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(url)
                .build();

        this.apiKey = apiKey;
    }

    public String buscarProcesso(String numeroProcesso) {

        String numeroLimpo = numeroProcesso
                .replace(".", "")
                .replace("-", "");

        Map<String, Object> body = Map.of(
                "query", Map.of(
                        "match", Map.of(
                                "numeroProcesso", numeroLimpo
                        )
                )
        );

        return restClient.post()
                .header("Authorization", "APIKey " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);
    }
}