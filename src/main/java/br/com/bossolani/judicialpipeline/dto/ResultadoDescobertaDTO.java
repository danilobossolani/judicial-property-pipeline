package br.com.bossolani.judicialpipeline.dto;

import br.com.bossolani.judicialpipeline.model.ExecucaoDescoberta;
import br.com.bossolani.judicialpipeline.model.OrigemExecucaoDescoberta;
import br.com.bossolani.judicialpipeline.model.StatusExecucaoDescoberta;

import java.time.LocalDateTime;

public record ResultadoDescobertaDTO(
        Long execucaoId,
        String fonte,
        LocalDateTime inicio,
        LocalDateTime termino,
        Long duracaoMs,
        OrigemExecucaoDescoberta origem,
        StatusExecucaoDescoberta status,
        int encontrados,
        int elegiveis,
        int importados,
        int duplicados,
        int descartados,
        int falhas,
        String erroResumo
) {

    public static ResultadoDescobertaDTO de(
            ExecucaoDescoberta execucao
    ) {

        return new ResultadoDescobertaDTO(
                execucao.getId(),
                execucao.getFonte(),
                execucao.getInicio(),
                execucao.getTermino(),
                execucao.getDuracaoMs(),
                execucao.getOrigem(),
                execucao.getStatus(),
                execucao.getTotalEncontrado(),
                execucao.getTotalElegivel(),
                execucao.getTotalImportado(),
                execucao.getTotalDuplicado(),
                execucao.getTotalDescartado(),
                execucao.getTotalFalha(),
                execucao.getErroResumo()
        );
    }

    public int duplicadosPorUrl() {

        return duplicados;
    }

    public int descartadosNaTriagem() {

        return descartados;
    }
}
