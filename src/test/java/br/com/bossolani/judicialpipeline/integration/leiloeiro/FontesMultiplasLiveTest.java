package br.com.bossolani.judicialpipeline.integration.leiloeiro;

import br.com.bossolani.judicialpipeline.integration.leiloeiro.djen.DjenCnjProvider;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteDescobertoDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.gl.GlLeiloesProvider;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.mega.MegaLeiloesProvider;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.publicjud.PublicJudProvider;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.spy.SpyLeiloesProvider;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.sublime.SublimeLeiloeiroProvider;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.sublime.SublimeLeiloesBrowser;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.sublime.SublimeLeiloesDiscoveryBrowser;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.sublime.SublimeLeiloesScraper;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.trt.Trt15ComunicacaoProvider;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.trt.Trt2ComunicacaoProvider;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.zuk.PortalZukProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@EnabledIfSystemProperty(named = "fontes.live", matches = "true")
class FontesMultiplasLiveTest {

    private static final ResilienciaFonteService RESILIENCIA =
            new ResilienciaFonteService(2, 500);

    @Test
    void deveDescobrirEColetarAoMenosUmLoteRealDaSublime() throws Exception {
        validarFonteComLotes(new SublimeLeiloeiroProvider(
                new SublimeLeiloesDiscoveryBrowser(),
                new SublimeLeiloesScraper(),
                new SublimeLeiloesBrowser(),
                RESILIENCIA
        ));
    }

    @Test
    void deveDescobrirEColetarAoMenosUmLoteRealDaMega() throws Exception {
        validarFonteComLotes(new MegaLeiloesProvider(RESILIENCIA, 30000));
    }

    @Test
    void deveDescobrirEColetarAoMenosUmLoteRealDaSpy() throws Exception {
        validarFonteComLotes(new SpyLeiloesProvider(RESILIENCIA, 30000));
    }

    @Test
    void deveDescobrirEColetarAoMenosUmEditalRealDoPublicJud() throws Exception {
        validarFonteComLotes(new PublicJudProvider(RESILIENCIA, 30000));
    }

    @Test
    void deveDescobrirEColetarAoMenosUmLoteRealDoPortalZuk() throws Exception {
        validarFonteComLotes(new PortalZukProvider(RESILIENCIA, 30000));
    }

    @Test
    void deveDescobrirEColetarAoMenosUmEditalRealDoTrt15() throws Exception {
        validarFonteComLotes(new Trt15ComunicacaoProvider(
                RESILIENCIA,
                30000,
                45,
                10
        ));
    }

    @Test
    void deveDescobrirEColetarAoMenosUmEditalRealDoTrt2() throws Exception {
        validarFonteComLotes(new Trt2ComunicacaoProvider(
                RESILIENCIA,
                30000,
                45,
                10
        ));
    }

    @Test
    void deveColetarLoteRealDeVotorantimDaGlLeiloes() throws Exception {
        GlLeiloesProvider provider = new GlLeiloesProvider(RESILIENCIA, 30000);
        ColetaLeiloeiroDTO coleta = provider.coletar(
                "https://www.glleiloes.com.br/item/5081/detalhes"
        );

        assertNotNull(coleta.lote().getNumeroProcesso());
        assertNotNull(coleta.lote().getComarca());
    }

    @Test
    void deveConsultarApiPublicaDoDjenSemFalha() throws Exception {
        DjenCnjProvider provider = new DjenCnjProvider(
                RESILIENCIA,
                30000,
                45,
                2
        );

        assertNotNull(provider.descobrirLotes());
    }

    @Test
    void deveConsultarListagemRealDaGlSemFalha() throws Exception {
        GlLeiloesProvider provider = new GlLeiloesProvider(RESILIENCIA, 30000);

        assertNotNull(provider.descobrirLotes());
    }

    private void validarFonteComLotes(LeiloeiroProvider provider) throws Exception {
        List<LoteDescobertoDTO> lotes = provider.descobrirLotes();

        assertFalse(
                lotes.isEmpty(),
                () -> provider.nome() + " não retornou imóveis elegíveis nas páginas reais"
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
