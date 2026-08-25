package br.com.bossolani.judicialpipeline.integration;

import br.com.bossolani.judicialpipeline.integration.datajud.dto.DataJudProcessoDTO;
import br.com.bossolani.judicialpipeline.integration.datajud.dto.DataJudResponse;
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

    public DataJudProcessoDTO buscarProcesso(String numeroProcesso) {

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

        DataJudResponse resposta = restClient.post()
                .header("Authorization", "APIKey " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(DataJudResponse.class);

        if (resposta == null
                || resposta.hits() == null
                || resposta.hits().hits() == null
                || resposta.hits().hits().isEmpty()) {

            throw new IllegalArgumentException(
                    "Processo não encontrado no DataJud"
            );
        }

        DataJudResponse.Source source =
                resposta.hits().hits().get(0).source();

        return new DataJudProcessoDTO(
                source.numeroProcesso(),
                source.tribunal(),
                source.grau(),
                source.dataAjuizamento(),

                source.orgaoJulgador() != null
                        ? source.orgaoJulgador().nome()
                        : null,

                source.classe() != null
                        ? source.classe().nome()
                        : null,

                source.sistema() != null
                        ? source.sistema().nome()
                        : null,

                source.formato() != null
                        ? source.formato().nome()
                        : null,

                source.dataHoraUltimaAtualizacao()
        );
    }
}