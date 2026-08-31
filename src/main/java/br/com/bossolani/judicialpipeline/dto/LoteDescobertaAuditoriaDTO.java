package br.com.bossolani.judicialpipeline.dto;

import br.com.bossolani.judicialpipeline.model.DecisaoLoteDescoberta;

public record LoteDescobertaAuditoriaDTO(
        Long id,
        String titulo,
        String cidade,
        String urlOriginal,
        String urlNormalizada,
        String numeroProcesso,
        DecisaoLoteDescoberta decisao,
        String motivo,
        Long imovelId
) {

    public String tituloFormatado() {

        return valorOuPadrao(
                titulo,
                "Lote sem título"
        );
    }

    public String cidadeFormatada() {

        return valorOuPadrao(
                cidade,
                "Cidade não informada"
        );
    }

    public String numeroProcessoFormatado() {

        if (numeroProcesso == null
                || numeroProcesso.isBlank()) {

            return "Processo não identificado";
        }


        String digitos =
                numeroProcesso.replaceAll(
                        "\\D",
                        ""
                );


        if (digitos.length() != 20) {
            return numeroProcesso;
        }


        return digitos.substring(0, 7)
                + "-" + digitos.substring(7, 9)
                + "." + digitos.substring(9, 13)
                + "." + digitos.substring(13, 14)
                + "." + digitos.substring(14, 16)
                + "." + digitos.substring(16, 20);
    }

    public String decisaoFormatada() {

        if (decisao == null) {
            return "Não classificado";
        }


        return switch (decisao) {
            case IMPORTADO -> "Importado";
            case DUPLICADO -> "Duplicado";
            case DESCARTADO -> "Descartado";
            case FALHA -> "Falha";
        };
    }

    public String decisaoCss() {

        return decisao == null
                ? "nao-classificado"
                : decisao.name()
                .toLowerCase();
    }

    private String valorOuPadrao(
            String valor,
            String padrao
    ) {

        return valor == null
                || valor.isBlank()
                ? padrao
                : valor;
    }
}
