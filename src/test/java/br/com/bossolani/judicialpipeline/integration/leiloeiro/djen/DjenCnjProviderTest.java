package br.com.bossolani.judicialpipeline.integration.leiloeiro.djen;

import br.com.bossolani.judicialpipeline.exception.LoteDescartadoException;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.ColetaLeiloeiroDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.ResilienciaFonteService;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteDescobertoDTO;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DjenCnjProviderTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String HASH = "AbCdEfGhIjKlMnOpQrStUvWxYz1234";
    private static final String PROCESSO_DIGITOS = "10123456720248260602";
    private static final String URL = "https://comunica.pje.jus.br/consulta/" + HASH
            + "?numeroProcesso=" + PROCESSO_DIGITOS;

    private static final Clock RELOGIO = Clock.fixed(
            Instant.parse("2026-09-03T15:00:00Z"),
            ZoneId.of("America/Sao_Paulo")
    );

    @Test
    void deveDescobrirSomenteEditalDeLeilaoComImovelNaRegiao() throws Exception {
        DjenCnjProvider provider = provider(parametros -> {
            if ("Votorantim/SP".equals(parametros.get("texto"))) {
                return resposta("[]");
            }
            return resposta("[" + itemJson(HASH, editalElegivel()) + ","
                    + itemJson("OutroHashComTamanhoValido123", falsoPositivo()) + "]");
        });

        List<LoteDescobertoDTO> lotes = provider.descobrirLotes();

        assertEquals(1, lotes.size());
        assertEquals("Sorocaba", lotes.getFirst().cidade());
        assertEquals("DJEN/CNJ (fonte oficial)", lotes.getFirst().fonte());
        assertEquals(URL, lotes.getFirst().url());
    }

    @Test
    void deveExtrairDadosDoEditalOficial() throws Exception {
        DjenCnjProvider provider = provider(
                parametros -> resposta("[" + itemJson(HASH, editalElegivel()) + "]")
        );

        ColetaLeiloeiroDTO coleta = provider.coletar(URL);

        assertEquals("1012345-67.2024.8.26.0602", coleta.lote().getNumeroProcesso());
        assertEquals("Terreno", coleta.lote().getTipo());
        assertEquals("Sorocaba", coleta.lote().getComarca());
        assertEquals("Rua das Acácias", coleta.lote().getEndereco());
        assertEquals("321", coleta.lote().getNumero());
        assertEquals("Jardim Europa", coleta.lote().getBairro());
        assertEquals(new BigDecimal("800000.00"), coleta.lote().getValorAvaliacao());
        assertEquals(LocalDateTime.of(2026, 9, 15, 14, 0), coleta.leilao().abertura1Praca());
        assertEquals(LocalDateTime.of(2026, 9, 20, 14, 0), coleta.leilao().fechamento1Praca());
        assertEquals(LocalDateTime.of(2026, 9, 30, 14, 0), coleta.leilao().fechamento2Praca());
        assertEquals(new BigDecimal("480000.00"), coleta.leilao().lanceMinimo());
        assertEquals(40, coleta.leilao().percentualDesconto());
        assertEquals(new BigDecimal("5"), coleta.leilao().comissaoPercentual());
        assertEquals("AGENDADO", coleta.leilao().status());
    }

    @Test
    void deveRejeitarCitacaoQueApenasMencionaLeilaoEOutraCidade() throws Exception {
        DjenCnjProvider provider = provider(
                parametros -> resposta("[" + itemJson(HASH, falsoPositivo()) + "]")
        );

        assertThrows(LoteDescartadoException.class, () -> provider.coletar(URL));
    }

    @Test
    void deveAceitarSomenteUrlOficialComProcesso() {
        DjenCnjProvider provider = provider(parametros -> resposta("[]"));

        assertTrue(provider.suporta(URI.create(URL)));
        assertFalse(provider.suporta(URI.create(URL.replace("https", "http"))));
        assertFalse(provider.suporta(URI.create(
                "https://comunica.pje.jus.br/consulta/" + HASH
        )));
    }

    private DjenCnjProvider provider(DjenCnjProvider.ApiClient client) {
        return new DjenCnjProvider(
                new ResilienciaFonteService(1, 0),
                5000,
                45,
                2,
                RELOGIO,
                client
        );
    }

    private JsonNode resposta(String itens) throws Exception {
        return OBJECT_MAPPER.readTree("{\"count\":1,\"items\":" + itens + "}");
    }

    private String itemJson(String hash, String texto) throws Exception {
        Map<String, Object> item = Map.of(
                "hash", hash,
                "numero_processo", PROCESSO_DIGITOS,
                "nomeOrgao", "3ª Vara Cível - Sorocaba",
                "texto", texto
        );
        return OBJECT_MAPPER.writeValueAsString(item);
    }

    private String editalElegivel() {
        return """
                <style>body { color: black; }</style>
                <p>EDITAL DE LEILÃO JUDICIAL ELETRÔNICO</p>
                <p>Processo nº 1012345-67.2024.8.26.0602.</p>
                <p>DESCRIÇÃO DO IMÓVEL: Terreno situado na Rua das Acácias, 321,
                Jardim Europa, Sorocaba/SP. Matrícula 12345.</p>
                <p>Valor de avaliação: R$ 800.000,00.</p>
                <p>1º LEILÃO: 15/09/2026 às 14:00 até 20/09/2026 às 14:00.</p>
                <p>2º LEILÃO: 21/09/2026 às 14:00 até 30/09/2026 às 14:00.</p>
                <p>Lance mínimo correspondente a 60% da avaliação.</p>
                <p>Comissão do leiloeiro: 5%.</p>
                """;
    }

    private String falsoPositivo() {
        return """
                <p>EDITAL DE CITAÇÃO - prazo de 20 dias.</p>
                <p>Processo nº 1012345-67.2024.8.26.0602.</p>
                <p>A parte citada reside em Sorocaba/SP e possui um imóvel.</p>
                <p>Outro processo menciona praça e leilão.</p>
                """;
    }
}
