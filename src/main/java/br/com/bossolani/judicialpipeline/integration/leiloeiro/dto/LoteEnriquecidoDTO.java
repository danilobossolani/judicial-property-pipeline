package br.com.bossolani.judicialpipeline.integration.leiloeiro.dto;

import br.com.bossolani.judicialpipeline.integration.datajud.dto.DataJudProcessoDTO;

public record LoteEnriquecidoDTO(
        LoteLeilaoDTO lote,
        DataJudProcessoDTO processo,
        boolean processoConfirmado
) {
}