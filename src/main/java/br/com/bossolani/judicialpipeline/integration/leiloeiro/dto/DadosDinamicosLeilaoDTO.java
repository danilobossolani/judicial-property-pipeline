package br.com.bossolani.judicialpipeline.integration.leiloeiro.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DadosDinamicosLeilaoDTO(

        LocalDateTime abertura1Praca,
        LocalDateTime fechamento1Praca,
        BigDecimal lanceInicial1Praca,

        LocalDateTime abertura2Praca,
        LocalDateTime fechamento2Praca,
        BigDecimal lanceInicial2Praca,

        Integer percentualDesconto,

        String status,
        String resultado,

        BigDecimal lanceMinimo,
        BigDecimal incremento,
        BigDecimal comissaoPercentual
) {
}