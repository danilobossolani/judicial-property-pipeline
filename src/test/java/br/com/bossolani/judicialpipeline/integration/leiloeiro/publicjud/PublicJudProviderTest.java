package br.com.bossolani.judicialpipeline.integration.leiloeiro.publicjud;

import br.com.bossolani.judicialpipeline.integration.leiloeiro.ColetaLeiloeiroDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.ResilienciaFonteService;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteDescobertoDTO;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PublicJudProviderTest {

    private static final String URL_EDITAL =
            "https://www.publicjud.com.br/edital/12345";

    private static final Clock RELOGIO = Clock.fixed(
            Instant.parse("2026-09-03T15:00:00Z"),
            ZoneId.of("America/Sao_Paulo")
    );

    @Test
    void deveDescobrirEditalUnitarioDeImovelSemDuplicar()
            throws Exception {

        PublicJudProvider provider = provider((url, formulario) -> {
            if (url.endsWith("/consulta")) {
                return Jsoup.parse("""
                        <html><body>
                          <a href="/edital/12345">Ver edital</a>
                          <a href="/edital/12345">Repetido</a>
                        </body></html>
                        """, url);
            }

            return Jsoup.parse(editalUnitario(), url);
        });

        List<LoteDescobertoDTO> lotes = provider.descobrirLotes();

        assertEquals(1, lotes.size());
        assertEquals("Sorocaba", lotes.getFirst().cidade());
        assertEquals(
                "PublicJud (editais judiciais)",
                lotes.getFirst().fonte()
        );
        assertEquals(URL_EDITAL, lotes.getFirst().url());
    }

    @Test
    void deveExtrairProcessoTerrenoEnderecoValoresEDatas()
            throws Exception {

        PublicJudProvider provider = provider(
                (url, formulario) -> Jsoup.parse(editalUnitario(), url)
        );

        ColetaLeiloeiroDTO coleta = provider.coletar(URL_EDITAL);

        assertEquals(
                "1001234-56.2024.8.26.0602",
                coleta.lote().getNumeroProcesso()
        );
        assertEquals("Terreno", coleta.lote().getTipo());
        assertEquals("Sorocaba", coleta.lote().getComarca());
        assertEquals("Rua das Acácias", coleta.lote().getEndereco());
        assertEquals("321", coleta.lote().getNumero());
        assertEquals("Jardim Europa", coleta.lote().getBairro());
        assertEquals(
                new BigDecimal("800000.00"),
                coleta.lote().getValorAvaliacao()
        );
        assertEquals(
                LocalDateTime.of(2026, 9, 15, 14, 0),
                coleta.leilao().fechamento1Praca()
        );
        assertEquals(
                new BigDecimal("480000.00"),
                coleta.leilao().lanceInicial2Praca()
        );
        assertEquals(40, coleta.leilao().percentualDesconto());
        assertEquals(new BigDecimal("5"), coleta.leilao().comissaoPercentual());
    }

    @Test
    void deveRejeitarEditalComMaisDeUmLote()
            throws Exception {

        String multiplos = editalUnitario().replace(
                "Processo: 1001234-56.2024.8.26.0602",
                "1 - Processo: 1001234-56.2024.8.26.0602 "
                        + "2 - Processo: 1009999-88.2024.8.26.0602"
        );

        PublicJudProvider provider = provider(
                (url, formulario) -> Jsoup.parse(multiplos, url)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> provider.coletar(URL_EDITAL)
        );
    }

    private String editalUnitario() {

        return """
                <html><body><table>
                  <tr><th>Código</th><td>ED-123</td></tr>
                  <tr><th>Cidade/UF</th><td>Sorocaba/SP</td></tr>
                  <tr><th>Vara</th><td>2ª Vara Cível de Sorocaba</td></tr>
                  <tr><th>Primeiro Leilão</th><td>15/09/2026 14:00:00</td></tr>
                  <tr><th>Último Leilão</th><td>30/09/2026 14:00:00</td></tr>
                  <tr><th>Situação</th><td>Publicado</td></tr>
                  <tr><th>Conteúdo</th><td>
                    Processo: 1001234-56.2024.8.26.0602. Terreno em Sorocaba.
                    Localização: Rua das Acácias, 321, Jardim Europa, Sorocaba - SP.
                    Valor de avaliação atualizado: R$ 800.000,00.
                    Lance mínimo correspondente a 60% da avaliação.
                    Comissão do leiloeiro: 5%.
                  </td></tr>
                </table></body></html>
                """;
    }

    private PublicJudProvider provider(
            PublicJudProvider.CarregadorPagina carregador
    ) {

        return new PublicJudProvider(
                new ResilienciaFonteService(1, 0),
                5000,
                carregador,
                RELOGIO
        );
    }
}
