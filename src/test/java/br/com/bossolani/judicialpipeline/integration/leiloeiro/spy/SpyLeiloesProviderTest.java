package br.com.bossolani.judicialpipeline.integration.leiloeiro.spy;

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

class SpyLeiloesProviderTest {

    private static final String URL_LOTE =
            "https://spyleiloes.com.br/leilao/12345/terreno-em-sorocaba";

    private static final Clock RELOGIO = Clock.fixed(
            Instant.parse("2026-09-03T15:00:00Z"),
            ZoneId.of("America/Sao_Paulo")
    );

    @Test
    void deveDescobrirLotesJudiciaisDasDuasCidadesSemDuplicar()
            throws Exception {

        String listagemSorocaba = """
                <html><body>
                  <a href="/leilao/12345/terreno-em-sorocaba"
                     title="Leilão de terreno em Sorocaba">
                    <h2>Terreno em Sorocaba</h2>
                    <p class="styles_adress">Rua das Flores, Sorocaba</p>
                  </a>
                  <a href="/leilao/12345/terreno-em-sorocaba">
                    <h2>Item repetido</h2>
                  </a>
                  <a href="?page=1">1</a>
                </body></html>
                """;

        String listagemVotorantim = """
                <html><body>
                  <a href="/leilao/67890/casa-em-votorantim">
                    <h2>Casa em Votorantim</h2>
                  </a>
                </body></html>
                """;

        SpyLeiloesProvider provider = provider(
                url -> Jsoup.parse(
                        url.contains("/votorantim/")
                                ? listagemVotorantim
                                : listagemSorocaba,
                        url
                )
        );

        List<LoteDescobertoDTO> lotes = provider.descobrirLotes();

        assertEquals(2, lotes.size());
        assertTrue(lotes.stream().allMatch(
                lote -> "SPY Leilões".equals(lote.fonte())
        ));
        assertTrue(lotes.stream().anyMatch(
                lote -> "Sorocaba".equals(lote.cidade())
        ));
        assertTrue(lotes.stream().anyMatch(
                lote -> "Votorantim".equals(lote.cidade())
        ));
    }

    @Test
    void deveExtrairTerrenoProcessoEnderecoAvaliacaoEPracas()
            throws Exception {

        String detalhe = """
                <html><head>
                  <script type="application/ld+json">
                    {
                      "@type": "RealEstateListing",
                      "mainEntity": {
                        "@type": "Land",
                        "address": {
                          "streetAddress": "Rua das Flores, 250, Jardim Europa, Sorocaba - SP",
                          "addressLocality": "Sorocaba"
                        }
                      }
                    }
                  </script>
                </head><body>
                  <h1>Leilão de terreno em Sorocaba</h1>
                  <p>Processo n° 1012345-67.2024.8.26.0602 - 3ª Vara Cível de Sorocaba.</p>
                  <span class="auctionPage_avalValue">R$ 500.000,00</span>
                  <div class="auctionPage_datesDiv">
                    <span class="auctionPage_h6Value">10/09/2026 14:00 • R$ 500.000,00</span>
                    <span class="auctionPage_h6Value">30/09/2026 14:00 • R$ 300.000,00</span>
                    <span>Desconto 40%</span>
                  </div>
                  <span class="auctionPage_h4LanceInicial">Lance inicial R$ 300.000,00</span>
                  <p>Comissão do leiloeiro: 5%</p>
                </body></html>
                """;

        SpyLeiloesProvider provider = provider(
                url -> Jsoup.parse(detalhe, url)
        );

        ColetaLeiloeiroDTO coleta = provider.coletar(URL_LOTE);

        assertEquals(
                "1012345-67.2024.8.26.0602",
                coleta.lote().getNumeroProcesso()
        );
        assertEquals("Terreno", coleta.lote().getTipo());
        assertEquals("Sorocaba", coleta.lote().getComarca());
        assertEquals("Rua das Flores", coleta.lote().getEndereco());
        assertEquals("250", coleta.lote().getNumero());
        assertEquals("Jardim Europa", coleta.lote().getBairro());
        assertEquals(
                new BigDecimal("500000.00"),
                coleta.lote().getValorAvaliacao()
        );
        assertEquals(
                LocalDateTime.of(2026, 9, 10, 14, 0),
                coleta.leilao().fechamento1Praca()
        );
        assertEquals(
                new BigDecimal("300000.00"),
                coleta.leilao().lanceInicial2Praca()
        );
        assertEquals(
                new BigDecimal("300000.00"),
                coleta.leilao().lanceMinimo()
        );
        assertEquals(40, coleta.leilao().percentualDesconto());
        assertEquals("AGENDADO", coleta.leilao().status());
    }

    @Test
    void deveAceitarSomenteUrlHttpsDeLoteDaSpy() {

        SpyLeiloesProvider provider = provider(
                url -> Jsoup.parse("", url)
        );

        assertTrue(provider.suporta(URI.create(URL_LOTE)));
        assertFalse(provider.suporta(URI.create(
                "http://spyleiloes.com.br/leilao/12345/terreno-em-sorocaba"
        )));
        assertFalse(provider.suporta(URI.create(
                "https://spyleiloes.com.br/imoveis-leilao/sp/sorocaba"
        )));
    }

    private SpyLeiloesProvider provider(
            SpyLeiloesProvider.CarregadorPagina carregador
    ) {

        return new SpyLeiloesProvider(
                new ResilienciaFonteService(1, 0),
                5000,
                carregador,
                RELOGIO
        );
    }
}
