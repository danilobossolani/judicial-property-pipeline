package br.com.bossolani.judicialpipeline.dto;

import java.util.List;

public record DescobertaAuditoriaDTO(
        List<ExecucaoDescobertaAuditoriaDTO> execucoes,
        ExecucaoDescobertaAuditoriaDTO ultimaExecucao,
        int totalExecucoes,
        int totalEncontrados,
        int totalImportados,
        int totalDuplicados,
        int totalDescartados,
        int totalFalhas,
        String erroCarregamento
) {

    public boolean possuiErroCarregamento() {

        return erroCarregamento != null
                && !erroCarregamento.isBlank();
    }
}
