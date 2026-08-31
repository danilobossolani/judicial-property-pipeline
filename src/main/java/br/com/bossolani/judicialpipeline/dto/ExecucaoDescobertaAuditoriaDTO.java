package br.com.bossolani.judicialpipeline.dto;

import br.com.bossolani.judicialpipeline.model.OrigemExecucaoDescoberta;
import br.com.bossolani.judicialpipeline.model.StatusExecucaoDescoberta;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public record ExecucaoDescobertaAuditoriaDTO(
        Long id,
        String fonte,
        LocalDateTime inicio,
        LocalDateTime termino,
        Long duracaoMs,
        OrigemExecucaoDescoberta origem,
        StatusExecucaoDescoberta status,
        int totalEncontrado,
        int totalElegivel,
        int totalImportado,
        int totalDuplicado,
        int totalDescartado,
        int totalFalha,
        String erroResumo,
        List<LoteDescobertaAuditoriaDTO> lotes
) {

    private static final DateTimeFormatter FORMATADOR_DATA =
            DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy 'às' HH:mm:ss"
            );

    public String inicioFormatado() {

        return formatarData(
                inicio,
                "Início não registrado"
        );
    }

    public String terminoFormatado() {

        return formatarData(
                termino,
                "Em andamento"
        );
    }

    public String duracaoFormatada() {

        if (duracaoMs == null) {
            return "Em andamento";
        }


        if (duracaoMs < 1000) {
            return duracaoMs + " ms";
        }


        long segundos =
                duracaoMs / 1000;

        long minutos =
                segundos / 60;

        long segundosRestantes =
                segundos % 60;


        return minutos > 0
                ? minutos + " min " + segundosRestantes + " s"
                : segundos + " s";
    }

    public String origemFormatada() {

        return origem == OrigemExecucaoDescoberta.AGENDADA
                ? "Agendada"
                : "Manual";
    }

    public String statusFormatado() {

        if (status == null) {
            return "Status não informado";
        }


        return switch (status) {
            case EM_EXECUCAO -> "Em execução";
            case CONCLUIDA -> "Concluída";
            case CONCLUIDA_COM_FALHAS -> "Concluída com falhas";
            case FALHOU -> "Falhou";
        };
    }

    public String statusCss() {

        return status == null
                ? "nao-informado"
                : status.name()
                .toLowerCase()
                .replace('_', '-');
    }

    public int totalProcessado() {

        return totalImportado
                + totalDuplicado
                + totalDescartado
                + totalFalha;
    }

    private String formatarData(
            LocalDateTime data,
            String padrao
    ) {

        return data == null
                ? padrao
                : data.format(
                FORMATADOR_DATA
        );
    }
}
