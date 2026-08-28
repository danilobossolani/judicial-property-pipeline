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

    public String numeroProcessoFormatado() {

        if (numeroProcesso == null) {
            return "Não informado";
        }

        String numeroLimpo =
                numeroProcesso.replaceAll("\\D", "");

        if (numeroLimpo.length() != 20) {
            return numeroProcesso;
        }

        return numeroLimpo.substring(0, 7)
                + "-"
                + numeroLimpo.substring(7, 9)
                + "."
                + numeroLimpo.substring(9, 13)
                + "."
                + numeroLimpo.substring(13, 14)
                + "."
                + numeroLimpo.substring(14, 16)
                + "."
                + numeroLimpo.substring(16, 20);
    }

    public String statusPipelineFormatado() {

        if (statusPipeline == null) {
            return "Identificado";
        }

        return switch (statusPipeline) {

            case IDENTIFICADO ->
                    "Identificado";

            case MONITORANDO_LEILAO ->
                    "Monitorando leilão";

            case AGUARDANDO_RESULTADO ->
                    "Aguardando resultado";

            case MONITORANDO_PROCESSO ->
                    "Monitorando processo";

            case EM_ANALISE ->
                    "Em análise";

            case OPORTUNIDADE ->
                    "Oportunidade";

            case DESCARTADO ->
                    "Descartado";

            case ENCERRADO ->
                    "Encerrado";
        };
    }

    public String statusLeilaoFormatado() {

        if (statusLeilao == null) {
            return "Desconhecido";
        }

        return switch (statusLeilao) {

            case AGENDADO ->
                    "Agendado";

            case EM_ANDAMENTO ->
                    "Em andamento";

            case ENCERRADO ->
                    "Encerrado";

            case SUSPENSO ->
                    "Suspenso";

            case CANCELADO ->
                    "Cancelado";

            case DESCONHECIDO ->
                    "Desconhecido";
        };
    }

    public String resultadoLeilaoFormatado() {

        if (resultadoLeilao == null) {
            return "Desconhecido";
        }

        return switch (resultadoLeilao) {

            case COM_LANCES ->
                    "Com lances";

            case SEM_LANCES ->
                    "Sem lances";

            case ARREMATADO ->
                    "Arrematado";

            case DESERTO ->
                    "Deserto";

            case DESCONHECIDO ->
                    "Desconhecido";
        };
    }
}