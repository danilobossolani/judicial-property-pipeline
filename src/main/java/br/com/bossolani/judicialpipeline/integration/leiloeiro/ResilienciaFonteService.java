package br.com.bossolani.judicialpipeline.integration.leiloeiro;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ResilienciaFonteService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    ResilienciaFonteService.class
            );

    private final int maxTentativas;

    private final long esperaInicialMs;


    public ResilienciaFonteService(
            @Value("${integracao.fontes.max-tentativas:3}")
            int maxTentativas,
            @Value("${integracao.fontes.espera-inicial-ms:400}")
            long esperaInicialMs
    ) {

        this.maxTentativas =
                Math.max(
                        1,
                        maxTentativas
                );

        this.esperaInicialMs =
                Math.max(
                        0,
                        esperaInicialMs
                );
    }


    public <T> T executar(
            String fonte,
            OperacaoFonte<T> operacao
    ) throws Exception {

        Exception ultimaFalha = null;


        for (int tentativa = 1;
             tentativa <= maxTentativas;
             tentativa++) {

            try {

                return operacao.executar();

            } catch (Exception exception) {

                ultimaFalha = exception;


                if (tentativa >= maxTentativas) {
                    break;
                }


                long espera =
                        esperaInicialMs * tentativa;


                log.warn(
                        "Falha transitória na fonte '{}'. Tentativa {}/{}. Nova tentativa em {} ms: {}",
                        fonte,
                        tentativa,
                        maxTentativas,
                        espera,
                        resumirErro(
                                exception
                        )
                );


                aguardar(
                        espera
                );
            }
        }


        throw ultimaFalha != null
                ? ultimaFalha
                : new IllegalStateException(
                "A operação da fonte não foi executada"
        );
    }


    private void aguardar(
            long espera
    ) throws InterruptedException {

        if (espera <= 0) {
            return;
        }


        try {

            Thread.sleep(
                    espera
            );

        } catch (InterruptedException exception) {

            Thread.currentThread()
                    .interrupt();


            throw exception;
        }
    }


    private String resumirErro(
            Exception exception
    ) {

        String mensagem =
                exception.getMessage();


        return mensagem == null
                || mensagem.isBlank()
                ? exception.getClass()
                .getSimpleName()
                : mensagem.replaceAll(
                "\\s+",
                " "
        ).trim();
    }


    @FunctionalInterface
    public interface OperacaoFonte<T> {

        T executar()
                throws Exception;
    }
}
