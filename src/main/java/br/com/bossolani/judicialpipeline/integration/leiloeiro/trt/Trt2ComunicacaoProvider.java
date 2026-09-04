package br.com.bossolani.judicialpipeline.integration.leiloeiro.trt;

import br.com.bossolani.judicialpipeline.integration.leiloeiro.ResilienciaFonteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
@Order(56)
public class Trt2ComunicacaoProvider extends TrtComunicacaoProvider {

    private static final String NOME = "TRT-2 (fonte oficial trabalhista)";

    @Autowired
    public Trt2ComunicacaoProvider(
            ResilienciaFonteService resiliencia,
            @Value("${integracao.fontes.timeout-ms:15000}") int timeoutMs,
            @Value("${integracao.trt.janela-dias:45}") int janelaDias,
            @Value("${integracao.trt.max-paginas:10}") int maxPaginas
    ) {
        super(
                NOME,
                "TRT2",
                "502",
                resiliencia,
                timeoutMs,
                janelaDias,
                maxPaginas
        );
    }

    Trt2ComunicacaoProvider(
            ResilienciaFonteService resiliencia,
            int timeoutMs,
            int janelaDias,
            int maxPaginas,
            Clock clock,
            ApiClient apiClient
    ) {
        super(
                NOME,
                "TRT2",
                "502",
                resiliencia,
                timeoutMs,
                janelaDias,
                maxPaginas,
                clock,
                apiClient
        );
    }
}
