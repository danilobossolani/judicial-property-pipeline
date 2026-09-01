package br.com.bossolani.judicialpipeline.integration.leiloeiro.mega;

import br.com.bossolani.judicialpipeline.integration.leiloeiro.ColetaLeiloeiroDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.ResilienciaFonteService;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteDescobertoDTO;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MegaLeiloesProviderTest {

    private static final String URL_LOTE =
            "https://www.megaleiloes.com.br/imoveis/apartamentos/sp/sorocaba/apartamento-vila-angelica-sorocaba-sp-j126858";

    @Test
    void deveDescobrirSomenteImoveisNasPaginasDasDuasCidades()
            throws Exception {

        String listagem = """
                <html><body>
                  <div class="summary">Exibindo 1 - 2 de 2 itens. Página 1 de 1.</div>
                  <div class="card">
                    <a class="card-title" href="%s">Apartamento em Sorocaba</a>
                    <div class="card-locality">Sorocaba, SP</div>
                  </div>
                  <div class="card">
                    <a class="card-title" href="https://www.megaleiloes.com.br/veiculos/carros/sp/sorocaba/carro-x123">Automóvel</a>
                    <div class="card-locality">Sorocaba, SP</div>
                  </div>
                </body></html>
                """.formatted(URL_LOTE);

        MegaLeiloesProvider provider =
                provider(url -> Jsoup.parse(listagem, url));

        List<LoteDescobertoDTO> lotes =
                provider.descobrirLotes();

        assertEquals(1, lotes.size());
        assertEquals("Mega Leilões", lotes.getFirst().fonte());
        assertEquals("Sorocaba", lotes.getFirst().cidade());
        assertEquals(URL_LOTE, lotes.getFirst().url());
    }

    @Test
    void deveExtrairProcessoEnderecoAvaliacaoPracasEStatus()
            throws Exception {

        String detalhe = """
                <html><body>
                  <h1 class="section-header">Apartamento 45 m² - Vila Angélica</h1>
                  <div class="locality item"><span class="value">Rua Dolores Bruno, 141, Vila Angélica, Sorocaba, SP</span></div>
                  <div class="process-number item"><span class="value">Processo 1011299-28.2022.8.26.0602</span></div>
                  <div class="jurisdiction item"><span class="value">5ª Vara Cível de Sorocaba</span></div>
                  <div class="rating-value item"><span class="value">R$ 191.066,07</span></div>
                  <div class="summary-info">
                    <div class="last-bid"><span class="value">R$ 120.000,00</span></div>
                    <div class="instance first">
                      <span class="card-first-instance-date">1ª Praça: 10/09/2026 às 14:00</span>
                      <span class="card-instance-value">R$ 191.066,07</span>
                    </div>
                    <div class="instance">
                      <span class="card-second-instance-date">2ª Praça: 30/09/2026 às 14:00</span>
                      <span class="card-instance-value">R$ 114.639,64</span>
                    </div>
                    <span class="instance-text">ENCERRADO - SEM LANCES</span>
                    <div class="increment"><span class="value">R$ 1.000,00</span></div>
                  </div>
                  <div class="defasagem"><span class="value">40%</span></div>
                  <div class="price"><span class="value">R$ 114.639,64</span></div>
                  <p>Comissão do leiloeiro: 5%</p>
                </body></html>
                """;

        MegaLeiloesProvider provider =
                provider(url -> Jsoup.parse(detalhe, url));

        ColetaLeiloeiroDTO coleta =
                provider.coletar(URL_LOTE);

        assertEquals(
                "1011299-28.2022.8.26.0602",
                coleta.lote().getNumeroProcesso()
        );
        assertEquals("Sorocaba", coleta.lote().getComarca());
        assertEquals("Rua Dolores Bruno", coleta.lote().getEndereco());
        assertEquals("141", coleta.lote().getNumero());
        assertEquals("Vila Angélica", coleta.lote().getBairro());
        assertEquals(
                new BigDecimal("191066.07"),
                coleta.lote().getValorAvaliacao()
        );
        assertEquals(
                LocalDateTime.of(2026, 9, 10, 14, 0),
                coleta.leilao().fechamento1Praca()
        );
        assertEquals(
                new BigDecimal("114639.64"),
                coleta.leilao().lanceInicial2Praca()
        );
        assertEquals("SEM LANCES", coleta.leilao().resultado());
        assertEquals(
                new BigDecimal("120000.00"),
                coleta.leilao().lanceMinimo()
        );
        assertEquals(new BigDecimal("5"), coleta.leilao().comissaoPercentual());
    }

    @Test
    void deveAceitarApenasUrlsHttpsDeImoveisDaMega() {

        MegaLeiloesProvider provider =
                provider(url -> Jsoup.parse("", url));

        assertTrue(provider.suporta(URI.create(URL_LOTE)));
        assertFalse(provider.suporta(
                URI.create("https://www.megaleiloes.com.br/veiculos/carros/sp/sorocaba/carro-x123")
        ));
        assertFalse(provider.suporta(
                URI.create("http://www.megaleiloes.com.br/imoveis/casa-j1")
        ));
    }

    private MegaLeiloesProvider provider(
            MegaLeiloesProvider.CarregadorPagina carregador
    ) {

        return new MegaLeiloesProvider(
                new ResilienciaFonteService(1, 0),
                5000,
                carregador
        );
    }
}
