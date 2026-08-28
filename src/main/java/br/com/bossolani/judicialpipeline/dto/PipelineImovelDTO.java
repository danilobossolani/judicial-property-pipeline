package br.com.bossolani.judicialpipeline.dto;

import br.com.bossolani.judicialpipeline.model.ResultadoLeilao;
import br.com.bossolani.judicialpipeline.model.StatusLeilao;
import br.com.bossolani.judicialpipeline.model.StatusPipeline;

import java.math.BigDecimal;

public record PipelineImovelDTO(

        Long imovelId,

        String tipo,

        String endereco,

        String numero,

        String bairro,

        String cidade,

        BigDecimal valorAvaliacao,

        String numeroProcesso,

        BigDecimal lanceMinimo,

        Integer percentualDesconto,

        StatusLeilao statusLeilao,

        ResultadoLeilao resultadoLeilao,

        StatusPipeline statusPipeline,

        String origemNome,

        String urlOrigem
) {
}