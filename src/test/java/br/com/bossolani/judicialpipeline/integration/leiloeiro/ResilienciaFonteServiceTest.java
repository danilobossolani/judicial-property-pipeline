package br.com.bossolani.judicialpipeline.integration.leiloeiro;

import br.com.bossolani.judicialpipeline.exception.LoteDescartadoException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResilienciaFonteServiceTest {

    @Test
    void naoDeveRepetirLoteDescartadoPorRegraDeNegocio() {

        AtomicInteger tentativas = new AtomicInteger();
        ResilienciaFonteService service =
                new ResilienciaFonteService(3, 0);

        assertThrows(
                LoteDescartadoException.class,
                () -> service.executar("Fonte de teste", () -> {
                    tentativas.incrementAndGet();
                    throw new LoteDescartadoException("Sem processo CNJ");
                })
        );

        assertEquals(1, tentativas.get());
    }

    @Test
    void deveRepetirFalhaTransitoriaAteObterSucesso()
            throws Exception {

        ResilienciaFonteService service =
                new ResilienciaFonteService(3, 0);
        AtomicInteger tentativas =
                new AtomicInteger();

        String resultado = service.executar(
                "Fonte de teste",
                () -> {
                    if (tentativas.incrementAndGet() < 3) {
                        throw new IllegalStateException("indisponível");
                    }

                    return "ok";
                }
        );

        assertEquals("ok", resultado);
        assertEquals(3, tentativas.get());
    }

    @Test
    void devePropagarUltimaFalhaAposLimite()
            throws Exception {

        ResilienciaFonteService service =
                new ResilienciaFonteService(2, 0);
        AtomicInteger tentativas =
                new AtomicInteger();

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.executar(
                                "Fonte de teste",
                                () -> {
                                    tentativas.incrementAndGet();
                                    throw new IllegalArgumentException("falha final");
                                }
                        )
                );

        assertEquals("falha final", exception.getMessage());
        assertEquals(2, tentativas.get());
    }
}
