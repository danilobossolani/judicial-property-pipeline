package br.com.bossolani.judicialpipeline.integration.leiloeiro.dto;

import br.com.bossolani.judicialpipeline.integration.datajud.dto.DataJudProcessoDTO;

public record LoteEnriquecidoDTO(
        LoteLeilaoDTO lote,
        DadosDinamicosLeilaoDTO leilao,
        DataJudProcessoDTO processo,
        boolean processoConfirmado,
        String fonte
) {

    public LoteEnriquecidoDTO(
            LoteLeilaoDTO lote,
            DadosDinamicosLeilaoDTO leilao,
            DataJudProcessoDTO processo,
            boolean processoConfirmado
    ) {

        this(
                lote,
                leilao,
                processo,
                processoConfirmado,
                null
        );
    }
}
