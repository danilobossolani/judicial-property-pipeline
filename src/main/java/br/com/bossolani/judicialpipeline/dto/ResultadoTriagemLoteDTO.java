package br.com.bossolani.judicialpipeline.dto;

public record ResultadoTriagemLoteDTO(
        boolean elegivel,
        String motivo,
        String urlNormalizada
) {
}
