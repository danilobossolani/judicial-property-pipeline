package br.com.bossolani.judicialpipeline.integration.leiloeiro.dto;

public record LoteDescobertoDTO(
        String url,
        String titulo,
        String cidade,
        String resumo,
        String fonte
) {

    public LoteDescobertoDTO(
            String url,
            String titulo,
            String cidade,
            String resumo
    ) {

        this(
                url,
                titulo,
                cidade,
                resumo,
                null
        );
    }
}
