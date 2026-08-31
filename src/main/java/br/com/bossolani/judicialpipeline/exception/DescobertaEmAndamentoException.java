package br.com.bossolani.judicialpipeline.exception;

public class DescobertaEmAndamentoException
        extends RuntimeException {

    public DescobertaEmAndamentoException() {

        super(
                "Já existe uma descoberta da Sublime Leilões em execução. Aguarde a conclusão antes de iniciar outra."
        );
    }
}
