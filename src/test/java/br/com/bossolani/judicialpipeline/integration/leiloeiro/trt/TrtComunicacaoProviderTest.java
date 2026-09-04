package br.com.bossolani.judicialpipeline.integration.leiloeiro.trt;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrtComunicacaoProviderTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String HASH = "AbCdEfGhIjKlMnOpQrStUvWxYz1234";
    private static final Clock RELOGIO = Clock.fixed(
            Instant.parse("2026-09-03T15:00:00Z"),
            ZoneId.of("America/Sao_Paulo")
    );

    @Test
    void deveSepararPautaDoTrt15EImportarSomenteImovelNaRegiao() throws Exception {
        JsonNode resposta = resposta(
                HASH,
                "00111112220265150001",
                "DIVEX - Campinas",
                editalTrt15()
        );
        Trt15ComunicacaoProvider provider = trt15(parametros -> {
            assertEquals("TRT15", parametros.get("siglaTribunal"));
            return resposta;
        });

        List<LoteDescobertoDTO> lotes = provider.descobrirLotes();

        assertEquals(1, lotes.size());
        assertEquals("Casa em Sorocaba", lotes.getFirst().titulo());
        assertEquals("Sorocaba", lotes.getFirst().cidade());
        assertEquals("TRT-15 (fonte oficial trabalhista)", lotes.getFirst().fonte());
        assertTrue(lotes.getFirst().url().contains("processoLote=00122223320265150002"));
        assertTrue(lotes.getFirst().url().contains("lote=2.1"));

        ColetaLeiloeiroDTO coleta = provider.coletar(lotes.getFirst().url());

        assertEquals("0012222-33.2026.5.15.0002", coleta.lote().getNumeroProcesso());
        assertEquals("Casa", coleta.lote().getTipo());
        assertEquals("Rua das Flores", coleta.lote().getEndereco());
        assertEquals("77", coleta.lote().getNumero());
        assertEquals("Centro", coleta.lote().getBairro());
        assertEquals(new BigDecimal("800000.00"), coleta.lote().getValorAvaliacao());
        assertEquals(new BigDecimal("400000.00"), coleta.leilao().lanceMinimo());
        assertEquals(50, coleta.leilao().percentualDesconto());
        assertEquals(LocalDateTime.of(2026, 9, 18, 14, 4), coleta.leilao().fechamento1Praca());
        assertEquals(new BigDecimal("5"), coleta.leilao().comissaoPercentual());
        assertEquals("AGENDADO", coleta.leilao().status());
    }

    @Test
    void deveColetarEditalUnitarioRealistaDoTrt2() throws Exception {
        JsonNode resposta = resposta(
                HASH,
                "10014180920215020205",
                "Centro de Apoio aos Leilões Judiciais Unificados",
                editalTrt2()
        );
        Trt2ComunicacaoProvider provider = trt2(parametros -> {
            assertEquals("TRT2", parametros.get("siglaTribunal"));
            return resposta;
        });

        List<LoteDescobertoDTO> lotes = provider.descobrirLotes();

        assertEquals(1, lotes.size());
        assertEquals("TRT-2 (fonte oficial trabalhista)", lotes.getFirst().fonte());
        assertTrue(provider.suporta(URI.create(lotes.getFirst().url())));
        assertFalse(trt15(parametros -> resposta).suporta(URI.create(lotes.getFirst().url())));

        ColetaLeiloeiroDTO coleta = provider.coletar(lotes.getFirst().url());

        assertEquals("1001418-09.2021.5.02.0205", coleta.lote().getNumeroProcesso());
        assertEquals("Sorocaba", coleta.lote().getComarca());
        assertEquals("Casa", coleta.lote().getTipo());
        assertEquals("Rua Júlio Prestes", coleta.lote().getEndereco());
        assertEquals("353", coleta.lote().getNumero());
        assertEquals(new BigDecimal("650000.00"), coleta.lote().getValorAvaliacao());
        assertEquals(new BigDecimal("450000.00"), coleta.leilao().lanceMinimo());
        assertEquals(31, coleta.leilao().percentualDesconto());
        assertEquals(LocalDateTime.of(2026, 10, 22, 10, 43), coleta.leilao().fechamento1Praca());
    }

    @Test
    void naoDeveConfundirCidadeDoForumComLocalDoImovel() throws Exception {
        String edital = """
                PODER JUDICIÁRIO - TRT-15 - SOROCABA/SP.
                EDITAL DE LEILÃO JUDICIAL.
                Processo 0019999-11.2026.5.15.0001.
                BENS: IMÓVEL DE MATRÍCULA 123. DESCRIÇÃO DO IMÓVEL: casa.
                Local dos bens: Rua Um, 10, Campinas/SP.
                Avaliação: R$ 200.000,00. Lance mínimo: R$ 100.000,00.
                No dia 20/10/2026, às 14:00 horas.
                """;
        JsonNode resposta = resposta(
                HASH,
                "00199991120265150001",
                "DIVEX - Sorocaba",
                edital
        );
        Trt15ComunicacaoProvider provider = trt15(parametros -> resposta);

        assertTrue(provider.descobrirLotes().isEmpty());
    }

    private Trt15ComunicacaoProvider trt15(TrtComunicacaoProvider.ApiClient client) {
        return new Trt15ComunicacaoProvider(
                new ResilienciaFonteService(1, 0),
                5000,
                45,
                10,
                RELOGIO,
                client
        );
    }

    private Trt2ComunicacaoProvider trt2(TrtComunicacaoProvider.ApiClient client) {
        return new Trt2ComunicacaoProvider(
                new ResilienciaFonteService(1, 0),
                5000,
                45,
                10,
                RELOGIO,
                client
        );
    }

    private JsonNode resposta(
            String hash,
            String processo,
            String orgao,
            String texto
    ) throws Exception {
        Map<String, Object> item = Map.of(
                "hash", hash,
                "numero_processo", processo,
                "numeroprocessocommascara", formatarProcesso(processo),
                "nomeOrgao", orgao,
                "siglaTribunal", processo.substring(13, 16).equals("515")
                        ? "TRT15"
                        : "TRT2",
                "ativo", true,
                "texto", texto
        );
        String json = OBJECT_MAPPER.writeValueAsString(item);
        return OBJECT_MAPPER.readTree(
                "{\"status\":\"success\",\"count\":1,\"items\":[" + json + "]}"
        );
    }

    private String formatarProcesso(String processo) {
        return processo.substring(0, 7) + "-" + processo.substring(7, 9)
                + "." + processo.substring(9, 13)
                + "." + processo.substring(13, 14)
                + "." + processo.substring(14, 16)
                + "." + processo.substring(16, 20);
    }

    private String editalTrt15() {
        return """
                EDITAL DE LEILÃO DA HASTA PÚBLICA Nº 04/2026.
                Os lances serão recebidos até o dia 18 de SETEMBRO de 2026, às 14H00min.
                Os lotes serão encerrados de modo escalonado, a cada 2 minutos.
                Comissão do Leiloeiro: 5%.
                1: 0011111-22.2026.5.15.0001 - EXE1 - Campinas
                1.1 Tipo do Bem: Veículo Identificação: automóvel.
                Localização: Avenida Central Número: 20, Bairro: Centro,
                Cidade: VOTORANTIM, UF: SP, CEP: 18110-000 Quantidade: 1.
                2: 0012222-33.2026.5.15.0002 - EXE2 - Campinas
                2.1 Tipo do Bem: Imóvel Identificação: Matrícula 456.
                Descrição: Casa térrea construída em terreno urbano.
                Localização: Rua das Flores Número: 77, Bairro: Centro,
                Cidade: SOROCABA, UF: SP, CEP: 18000-000 Quantidade: 1.
                Valor Total Penhorado: R$ 800.000,00.
                Valor Lance Mínimo (50%): R$ 400.000,00.
                """;
    }

    private String editalTrt2() {
        return """
                PODER JUDICIÁRIO JUSTIÇA DO TRABALHO TRT DA 2ª REGIÃO.
                Edital de Leilão Judicial Unificado.
                Processo nº 1001418-09.2021.5.02.0205.
                No dia 22/10/2026, às 10:43 horas, através do portal do leiloeiro,
                serão levados a leilão judicial os seguintes BENS:
                NUA PROPRIEDADE DO IMÓVEL DE MATRÍCULA 148.944 DO 1º CARTÓRIO
                DE REGISTRO DE IMÓVEIS DE SOROCABA/SP.
                DESCRIÇÃO: A casa sob o nº 353, da Rua Júlio Prestes.
                Avaliação: R$ 650.000,00.
                Local dos bens: Rua Júlio Prestes, 353, Sorocaba/SP.
                Total da avaliação: R$ 650.000,00.
                Lance mínimo do leilão: R$ 450.000,00.
                Comissão do(a) Leiloeiro(a): 5%.
                """;
    }
}
