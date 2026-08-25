package br.com.bossolani.judicialpipeline.integration.datajud.dto;

public record DataJudProcessoDTO(
        String numeroProcesso,
        String tribunal,
        String grau,
        String dataAjuizamento,
        String orgaoJulgador,
        String classe,
        String sistema,
        String formato,
        String ultimaAtualizacao
) {
}