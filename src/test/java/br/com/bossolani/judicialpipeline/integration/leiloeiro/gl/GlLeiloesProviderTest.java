package br.com.bossolani.judicialpipeline.integration.leiloeiro.gl;

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

class GlLeiloesProviderTest {

    private static final String URL_LOTE =
            "https://www.glleiloes.com.br/item/5081/detalhes";

    private static final Clock RELOGIO = Clock.fixed(
            Instant.parse("2026-09-03T15:00:00Z"),
            ZoneId.of("America/Sao_Paulo")
    );

    @Test
    void deveDescobrirSomenteImoveisDaRegiaoSemDuplicar() throws Exception {
        String listagem = """
                <html><body><div class="lista-lotes">
                  <div class="lote card">
                    <h4>Terreno judicial em Sorocaba</h4>
                    <p>Cidade: Sorocaba/SP Endereço: Rua Um, 10</p>
                    <a href="/item/5081/detalhes?page=1">Detalhes do lote</a>
                    <a href="/item/5081/detalhes?page=1">Link repetido</a>
                  </div>
                  <div class="lote card">
                    <h4>Casa fora da região</h4>
                    <p>Cidade: Tatuí/SP</p>
                    <a href="/item/5090/detalhes?page=1">Detalhes do lote</a>
                  </div>
                </div></body></html>
                """;
        GlLeiloesProvider provider = provider(url -> Jsoup.parse(listagem, url));

        List<LoteDescobertoDTO> lotes = provider.descobrirLotes();

        assertEquals(1, lotes.size());
        assertEquals("Sorocaba", lotes.getFirst().cidade());
        assertEquals("GL Leilões", lotes.getFirst().fonte());
        assertEquals(URL_LOTE, lotes.getFirst().url());
    }

    @Test
    void deveExtrairSobradoProcessoEnderecoValoresDatasEStatus() throws Exception {
        String detalhe = """
                <html><head>
                  <title>Direitos de Sobrado de 116,24 m2 - Vila Votorantim I - Votorantim/SP - Lote 001 (ID 5081) :: GL Leilões</title>
                </head><body><div class="detalhes-lote">
                  <span class="label_lote sem_licitante">Sem Licitante</span>
                  <div>LANCE INICIAL R$ 304.623,27</div>
                  <div>+ Comissão (5%): R$ 15.231,16</div>
                  <div>Incremento Mínimo: R$ 3.089,24</div>
                  <p>Data de abertura para lances: 09/07/2026 às 10:00</p>
                  <p>Data 1º Leilão: 09/07/2026 10:00 Lance Inicial:</p>
                  <p>Data 2º Leilão: 30/07/2026 10:00 Lance Inicial: R$ 304.623,27</p>
                  <p>Valor de Avaliação: R$ 280.000,00</p>
                  <p>Processo: 0005313-15.2020.8.26.0624</p>
                  <p>Vara: 2ª Vara Cível do Foro da Comarca de Tatuí/SP Comarca: Tatuí/SP</p>
                  <p>Cidade: Votorantim/SP</p>
                  <p>Endereço: Rua Victorio Zanchetta, 478 Descrição: DIREITOS de um SOBRADO</p>
                  <p>Localização do Imóvel Endereço: Rua Victorio Zanchetta, 478 - Vila Votorantim I Cidade: Votorantim / SP</p>
                </div></body></html>
                """;
        GlLeiloesProvider provider = provider(url -> Jsoup.parse(detalhe, url));

        ColetaLeiloeiroDTO coleta = provider.coletar(URL_LOTE + "?page=1");

        assertEquals("0005313-15.2020.8.26.0624", coleta.lote().getNumeroProcesso());
        assertEquals("Sobrado", coleta.lote().getTipo());
        assertEquals("Votorantim", coleta.lote().getComarca());
        assertEquals("Rua Victorio Zanchetta", coleta.lote().getEndereco());
        assertEquals("478", coleta.lote().getNumero());
        assertEquals("Vila Votorantim I", coleta.lote().getBairro());
        assertEquals(new BigDecimal("280000.00"), coleta.lote().getValorAvaliacao());
        assertEquals(LocalDateTime.of(2026, 7, 30, 10, 0), coleta.leilao().fechamento2Praca());
        assertEquals(new BigDecimal("304623.27"), coleta.leilao().lanceMinimo());
        assertEquals(new BigDecimal("3089.24"), coleta.leilao().incremento());
        assertEquals(new BigDecimal("5"), coleta.leilao().comissaoPercentual());
        assertEquals("ENCERRADO", coleta.leilao().status());
        assertEquals("SEM LANCES", coleta.leilao().resultado());
    }

    @Test
    void deveAceitarSomenteUrlHttpsDeDetalhe() {
        GlLeiloesProvider provider = provider(url -> Jsoup.parse("", url));

        assertTrue(provider.suporta(URI.create(URL_LOTE + "?page=2")));
        assertFalse(provider.suporta(URI.create(URL_LOTE.replace("https", "http"))));
        assertFalse(provider.suporta(URI.create("https://www.glleiloes.com.br/lotes/imovel")));
    }

    private GlLeiloesProvider provider(GlLeiloesProvider.CarregadorPagina carregador) {
        return new GlLeiloesProvider(
                new ResilienciaFonteService(1, 0),
                5000,
                RELOGIO,
                carregador
        );
    }
}
