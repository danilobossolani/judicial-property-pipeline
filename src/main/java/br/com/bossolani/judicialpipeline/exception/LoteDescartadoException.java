package br.com.bossolani.judicialpipeline.exception;

public class LoteDescartadoException
        extends IllegalArgumentException {

    public LoteDescartadoException(
            String mensagem
    ) {

        super(
                mensagem
        );
    }
}
