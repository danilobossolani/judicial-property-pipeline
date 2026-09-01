package br.com.bossolani.judicialpipeline.integration.leiloeiro;

import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.DadosDinamicosLeilaoDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteLeilaoDTO;

public record ColetaLeiloeiroDTO(
        LoteLeilaoDTO lote,
        DadosDinamicosLeilaoDTO leilao
) {
}
