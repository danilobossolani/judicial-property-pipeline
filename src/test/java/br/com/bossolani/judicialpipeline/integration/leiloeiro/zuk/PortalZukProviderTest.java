package br.com.bossolani.judicialpipeline.integration.leiloeiro.zuk;

import br.com.bossolani.judicialpipeline.integration.leiloeiro.ColetaLeiloeiroDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.ResilienciaFonteService;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteDescobertoDTO;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalZukProviderTest {

    private static final String URL_LOTE =
            "https://www.portalzuk.com.br/imovel/sp/sorocaba/jardim-europa/rua-das-acacias-321/37053-231141";

    private static final Clock RELOGIO = Clock.fixed(
            Instant.parse("2026-09-03T15:00:00Z"),
            ZoneId.of("America/Sao_Paulo")
    );

    @Test
    void deveDescobrirImoveisNasDuasCidadesSemDuplicar() throws Exception {
        String sorocaba = """
                <html><body>
                  <div class="card-property">
                    <a href="/imovel/sp/sorocaba/jardim-europa/rua-das-acacias-321/37053-231141"
                       title="Terreno em Sorocaba">Ver imóvel</a>
                    <p class="card-property-address">Rua das Acácias - Sorocaba/SP</p>
                  </div>
                  <div class="card-property">
                    <a href="/imovel/sp/sorocaba/jardim-europa/rua-das-acacias-321/37053-231141">Duplicado</a>
                  </div>
                </body></html>
                """;
        String votorantim = """
                <html><body><div class="card-property">
                  <a href="/imovel/sp/votorantim/centro/rua-maria-10/37054-231142"
                     title="Casa em Votorantim">Ver imóvel</a>
                </div></body></html>
                """;

        PortalZukProvider provider = provider(caminho -> Jsoup.parse(
                caminho.contains("votorantim") ? votorantim : sorocaba,
                "https://www.portalzuk.com.br" + caminho
        ));

        List<LoteDescobertoDTO> lotes = provider.descobrirLotes();

        assertEquals(2, lotes.size());
        assertTrue(lotes.stream().allMatch(lote -> "Portal Zuk".equals(lote.fonte())));
        assertTrue(lotes.stream().anyMatch(lote -> "Sorocaba".equals(lote.cidade())));
        assertTrue(lotes.stream().anyMatch(lote -> "Votorantim".equals(lote.cidade())));
    }

    @Test
    void deveExtrairProcessoEnderecoValoresDatasEComissao() throws Exception {
        String detalhe = """
                <html><head>
                  <meta property="og:title" content="Terreno em Sorocaba | Zuk" />
                </head><body>
                  <div class="property-address">Rua das Acácias, 321 - Jardim Europa - Sorocaba/SP</div>
                  <div class="property-info">Processo 1012345-67.2024.8.26.0602</div>
                  <div class="card-action-item">1º Leilão 15/09/26 às 14h00 R$ 800.000,00</div>
                  <div class="card-action-item">2º Leilão 30/09/26 às 14h00 60% R$ 480.000,00</div>
                  <div class="card-action">Leilão aberto</div>
                  <p>Comissão do leiloeiro: 5%</p>
                </body></html>
                """;
        PortalZukProvider provider = provider(url -> Jsoup.parse(detalhe, url));

        ColetaLeiloeiroDTO coleta = provider.coletar(URL_LOTE);

        assertEquals("1012345-67.2024.8.26.0602", coleta.lote().getNumeroProcesso());
        assertEquals("Terreno", coleta.lote().getTipo());
        assertEquals("Sorocaba", coleta.lote().getComarca());
        assertEquals("Rua das Acácias", coleta.lote().getEndereco());
        assertEquals("321", coleta.lote().getNumero());
        assertEquals("Jardim Europa", coleta.lote().getBairro());
        assertEquals(new BigDecimal("800000.00"), coleta.lote().getValorAvaliacao());
        assertEquals(LocalDateTime.of(2026, 9, 15, 14, 0), coleta.leilao().fechamento1Praca());
        assertEquals(new BigDecimal("480000.00"), coleta.leilao().lanceInicial2Praca());
        assertEquals(new BigDecimal("800000.00"), coleta.leilao().lanceMinimo());
        assertEquals(40, coleta.leilao().percentualDesconto());
        assertEquals(new BigDecimal("5"), coleta.leilao().comissaoPercentual());
        assertEquals("AGENDADO", coleta.leilao().status());
    }

    @Test
    void deveAceitarSomenteUrlHttpsDeImovel() {
        PortalZukProvider provider = provider(url -> Jsoup.parse("", url));

        assertTrue(provider.suporta(URI.create(URL_LOTE)));
        assertFalse(provider.suporta(URI.create(URL_LOTE.replace("https", "http"))));
        assertFalse(provider.suporta(URI.create("https://www.portalzuk.com.br/leilao-de-imoveis")));
    }

    private PortalZukProvider provider(PortalZukProvider.CarregadorPagina carregador) {
        return new PortalZukProvider(
                new ResilienciaFonteService(1, 0),
                5000,
                RELOGIO,
                carregador
        );
    }
}
