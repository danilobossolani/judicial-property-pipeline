package br.com.bossolani.judicialpipeline.integration.leiloeiro.sublime;

import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.DadosDinamicosLeilaoDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SublimeLeiloesBrowserTest {

    private final SublimeLeiloesBrowser browser =
            new SublimeLeiloesBrowser();


    @Test
    void deveReconhecerLoteAguardandoInicio() {

        String texto = """
                1º Praça
                Abertura: 31/08/2026 - 09:00
                Fechamento: 03/09/2026 - 15:00
                Lance Inicial: R$ 368.609,26
                2º Praça
                Abertura: 03/09/2026 - 15:00
                Fechamento: 24/09/2026 - 15:00
                Lance Inicial: R$ 184.304,63
                ❚ AGUARDANDO INÍCIO
                Informações
                """;


        DadosDinamicosLeilaoDTO dados =
                browser.extrairDados(
                        texto
                );


        assertEquals(
                "AGUARDANDO INÍCIO",
                dados.status()
        );
    }
}
