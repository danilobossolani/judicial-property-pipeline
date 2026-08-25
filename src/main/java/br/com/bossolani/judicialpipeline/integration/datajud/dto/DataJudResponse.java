package br.com.bossolani.judicialpipeline.integration.datajud.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DataJudResponse(
        Hits hits
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Hits(
            List<Hit> hits
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Hit(
            @JsonProperty("_source")
            Source source
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Source(
            String numeroProcesso,
            String tribunal,
            String grau,
            String dataAjuizamento,
            OrgaoJulgador orgaoJulgador,
            ItemNome classe,
            ItemNome sistema,
            ItemNome formato,
            String dataHoraUltimaAtualizacao
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrgaoJulgador(
            String nome
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ItemNome(
            String nome
    ) {
    }
}