package br.com.bossolani.judicialpipeline.integration.leiloeiro;

import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteDescobertoDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.publicjud.PublicJudProvider;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.spy.SpyLeiloesProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@EnabledIfSystemProperty(named = "fontes.live", matches = "true")
class FontesMultiplasLiveTest {

    @Test
    void deveDescobrirEColetarAoMenosUmLoteRealDaSpy()
            throws Exception {

        SpyLeiloesProvider provider = new SpyLeiloesProvider(
                new ResilienciaFonteService(2, 500),
                30000
        );

        validarFonte(provider);
    }

    @Test
    void deveDescobrirEColetarAoMenosUmEditalRealDoPublicJud()
            throws Exception {

        PublicJudProvider provider = new PublicJudProvider(
                new ResilienciaFonteService(2, 500),
                30000
        );

        validarFonte(provider);
    }

    private void validarFonte(
            LeiloeiroProvider provider
    ) throws Exception {

        List<LoteDescobertoDTO> lotes = provider.descobrirLotes();

        assertFalse(
                lotes.isEmpty(),
                () -> provider.nome()
                        + " não retornou imóveis elegíveis nas páginas reais"
        );

        Exception ultimaFalha = null;

        for (LoteDescobertoDTO lote : lotes.stream().limit(10).toList()) {
            try {
                ColetaLeiloeiroDTO coleta = provider.coletar(lote.url());
                assertNotNull(coleta.lote().getNumeroProcesso());
                assertNotNull(coleta.lote().getComarca());
                return;

            } catch (Exception exception) {
                ultimaFalha = exception;
            }
        }

        throw new AssertionError(
                provider.nome()
                        + " descobriu anúncios, mas os dez primeiros não puderam ser coletados",
                ultimaFalha
        );
    }
}
