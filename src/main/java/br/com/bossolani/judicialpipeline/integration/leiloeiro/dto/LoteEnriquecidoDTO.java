package br.com.bossolani.judicialpipeline.integration.leiloeiro.dto;

import br.com.bossolani.judicialpipeline.integration.datajud.dto.DataJudProcessoDTO;
import br.com.bossolani.judicialpipeline.model.FonteTipo;

public record LoteEnriquecidoDTO(
        LoteLeilaoDTO lote,
        DadosDinamicosLeilaoDTO leilao,
        DataJudProcessoDTO processo,
        boolean processoConfirmado,
        String fonte,
        FonteTipo fonteTipo
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
                null,
                FonteTipo.LEILOEIRO_OFICIAL
        );
    }

    public LoteEnriquecidoDTO(
            LoteLeilaoDTO lote,
            DadosDinamicosLeilaoDTO leilao,
            DataJudProcessoDTO processo,
            boolean processoConfirmado,
            String fonte
    ) {
        this(
                lote,
                leilao,
                processo,
                processoConfirmado,
                fonte,
                FonteTipo.LEILOEIRO_OFICIAL
        );
    }
}
