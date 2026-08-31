package br.com.bossolani.judicialpipeline.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public record CentralAuditoriaDTO(
        List<EventoAuditoriaDTO> eventos,
        int totalEventos,
        int totalManuais,
        int totalAutomaticos,
        long totalImoveisImpactados,
        LocalDateTime ultimaAtividade,
        DescobertaAuditoriaDTO descoberta
) {

    private static final DateTimeFormatter FORMATADOR_DATA =
            DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy 'às' HH:mm"
            );

    public String ultimaAtividadeFormatada() {

        if (ultimaAtividade == null) {
            return "Nenhuma atividade registrada";
        }


        return ultimaAtividade.format(
                FORMATADOR_DATA
        );
    }
}
