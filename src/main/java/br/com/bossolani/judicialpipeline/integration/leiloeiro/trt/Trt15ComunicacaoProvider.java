package br.com.bossolani.judicialpipeline.integration.leiloeiro.trt;

import br.com.bossolani.judicialpipeline.integration.leiloeiro.ResilienciaFonteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
@Order(55)
public class Trt15ComunicacaoProvider extends TrtComunicacaoProvider {

    private static final String NOME = "TRT-15 (fonte oficial trabalhista)";

    @Autowired
    public Trt15ComunicacaoProvider(
            ResilienciaFonteService resiliencia,
            @Value("${integracao.fontes.timeout-ms:15000}") int timeoutMs,
            @Value("${integracao.trt.janela-dias:45}") int janelaDias,
            @Value("${integracao.trt.max-paginas:10}") int maxPaginas
    ) {
        super(
                NOME,
                "TRT15",
                "515",
                resiliencia,
                timeoutMs,
                janelaDias,
                maxPaginas
        );
    }

    Trt15ComunicacaoProvider(
            ResilienciaFonteService resiliencia,
            int timeoutMs,
            int janelaDias,
            int maxPaginas,
            Clock clock,
            ApiClient apiClient
    ) {
        super(
                NOME,
                "TRT15",
                "515",
                resiliencia,
                timeoutMs,
                janelaDias,
                maxPaginas,
                clock,
                apiClient
        );
    }
}
