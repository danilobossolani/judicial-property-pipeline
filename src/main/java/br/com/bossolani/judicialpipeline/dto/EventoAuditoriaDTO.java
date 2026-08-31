package br.com.bossolani.judicialpipeline.dto;

import br.com.bossolani.judicialpipeline.model.ResultadoLeilao;
import br.com.bossolani.judicialpipeline.model.StatusLeilao;
import br.com.bossolani.judicialpipeline.model.StatusPipeline;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record EventoAuditoriaDTO(

        LocalDateTime dataEvento,
        Long imovelId,
        String numeroProcesso,
        String tipoImovel,
        String bairro,
        String cidade,
        StatusPipeline statusPipeline,
        StatusLeilao statusLeilao,
        ResultadoLeilao resultadoLeilao,
        String origem,
        String descricao

) {

    private static final DateTimeFormatter FORMATADOR_DATA =
            DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy 'às' HH:mm"
            );


    public String dataEventoFormatada() {

        if (dataEvento == null) {
            return "Data não informada";
        }

        return dataEvento.format(
                FORMATADOR_DATA
        );
    }


    public String dataEventoIso() {

        if (dataEvento == null) {
            return "";
        }

        return dataEvento.toString();
    }


    public String numeroProcessoFormatado() {

        if (numeroProcesso == null
                || numeroProcesso.isBlank()) {

            return "Processo não informado";
        }


        String numeroLimpo =
                numeroProcesso.replaceAll(
                        "\\D",
                        ""
                );


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


    public String localizacaoFormatada() {

        if (bairro != null
                && !bairro.isBlank()
                && cidade != null
                && !cidade.isBlank()) {

            return bairro + " — " + cidade;
        }


        if (cidade != null
                && !cidade.isBlank()) {

            return cidade;
        }


        return "Localização não informada";
    }


    public String tipoImovelFormatado() {

        if (tipoImovel == null
                || tipoImovel.isBlank()) {

            return "Imóvel";
        }

        return tipoImovel;
    }


    public String categoria() {

        if (origem != null
                && origem.toLowerCase()
                .contains("manual")) {

            return "MANUAL";
        }

        return "AUTOMATICA";
    }


    public String categoriaFormatada() {

        return categoria().equals("MANUAL")
                ? "Intervenção manual"
                : "Atualização automática";
    }


    public String statusPipelineFormatado() {

        if (statusPipeline == null) {
            return "Não informado";
        }


        return switch (statusPipeline) {
            case IDENTIFICADO -> "Identificado";
            case MONITORANDO_LEILAO -> "Monitorando leilão";
            case AGUARDANDO_RESULTADO -> "Aguardando resultado";
            case MONITORANDO_PROCESSO -> "Monitorando processo";
            case EM_ANALISE -> "Em análise";
            case OPORTUNIDADE -> "Oportunidade";
            case DESCARTADO -> "Descartado";
            case ENCERRADO -> "Encerrado";
        };
    }


    public String statusLeilaoFormatado() {

        if (statusLeilao == null) {
            return "Não informado";
        }


        return switch (statusLeilao) {
            case AGENDADO -> "Agendado";
            case EM_ANDAMENTO -> "Em andamento";
            case ENCERRADO -> "Encerrado";
            case SUSPENSO -> "Suspenso";
            case CANCELADO -> "Cancelado";
            case DESCONHECIDO -> "Desconhecido";
        };
    }


    public String resultadoLeilaoFormatado() {

        if (resultadoLeilao == null) {
            return "Não informado";
        }


        return switch (resultadoLeilao) {
            case COM_LANCES -> "Com lances";
            case SEM_LANCES -> "Sem lances";
            case ARREMATADO -> "Arrematado";
            case DESERTO -> "Deserto";
            case DESCONHECIDO -> "Desconhecido";
        };
    }


    public String descricaoFormatada() {

        if (descricao == null
                || descricao.isBlank()) {

            return "Estado do acompanhamento registrado.";
        }


        if (descricao.startsWith("Leilão:")) {

            return "Leilão: "
                    + statusLeilaoFormatado()
                    + " · Resultado: "
                    + resultadoLeilaoFormatado();
        }


        return descricao;
    }
}
