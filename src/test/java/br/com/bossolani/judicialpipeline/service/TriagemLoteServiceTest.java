package br.com.bossolani.judicialpipeline.service;

import br.com.bossolani.judicialpipeline.dto.ResultadoTriagemLoteDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteDescobertoDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TriagemLoteServiceTest {

    private final TriagemLoteService service =
            new TriagemLoteService();

    @Test
    void deveAceitarImovelEmSorocaba() {

        LoteDescobertoDTO lote =
                new LoteDescobertoDTO(
                        "https://www.sublimeleiloes.com.br/lote/casa-em-sorocaba/2405/",
                        "Casa em Sorocaba",
                        "Sorocaba",
                        "Residenciais Terreno de 405m²"
                );


        ResultadoTriagemLoteDTO resultado =
                service.avaliar(
                        lote
                );


        assertTrue(
                resultado.elegivel()
        );

        assertEquals(
                "Imóvel localizado em Sorocaba ou Votorantim.",
                resultado.motivo()
        );
    }

    @Test
    void deveAceitarImovelEmVotorantimComAcentosNoTitulo() {

        LoteDescobertoDTO lote =
                new LoteDescobertoDTO(
                        "https://www.sublimeleiloes.com.br/lote/area-em-votorantim/9999/",
                        "Área em Votorantim",
                        "Votorantim",
                        "Terrenos"
                );


        assertTrue(
                service.elegivel(
                        lote
                )
        );
    }

    @Test
    void deveRecusarBemMovelComMotivoObjetivo() {

        LoteDescobertoDTO lote =
                new LoteDescobertoDTO(
                        "https://www.sublimeleiloes.com.br/lote/honda-cg-125/9999/",
                        "Honda CG 125",
                        "Sorocaba",
                        "Veículos Motos"
                );


        ResultadoTriagemLoteDTO resultado =
                service.avaliar(
                        lote
                );


        assertFalse(
                resultado.elegivel()
        );

        assertEquals(
                "Bem móvel identificado na descrição do lote.",
                resultado.motivo()
        );
    }

    @Test
    void deveRecusarAcaoDeDespejoMesmoQuandoMencionaImovel() {

        LoteDescobertoDTO lote =
                new LoteDescobertoDTO(
                        "https://www.sublimeleiloes.com.br/lote/acao-de-despejo/9998/",
                        "Ação de despejo de imóvel comercial",
                        "Sorocaba",
                        "Direitos decorrentes de ação judicial"
                );


        ResultadoTriagemLoteDTO resultado =
                service.avaliar(
                        lote
                );


        assertFalse(
                resultado.elegivel()
        );

        assertEquals(
                "Ação de despejo não representa oportunidade imobiliária.",
                resultado.motivo()
        );
    }

    @Test
    void deveRecusarImovelForaDaRegiaoPermitidaComMotivo() {

        LoteDescobertoDTO lote =
                new LoteDescobertoDTO(
                        "https://www.sublimeleiloes.com.br/lote/casa-em-cotia/3383/",
                        "Casa em Cotia",
                        "Cotia",
                        "Residenciais"
                );


        ResultadoTriagemLoteDTO resultado =
                service.avaliar(
                        lote
                );


        assertFalse(
                resultado.elegivel()
        );

        assertEquals(
                "Cidade fora do escopo: somente Sorocaba e Votorantim são elegíveis.",
                resultado.motivo()
        );
    }

    @Test
    void deveCanonicalizarUrlEIgnorarConsultaEFragmento() {

        String normalizada =
                service.normalizarUrl(
                        "https://WWW.SUBLIMELEILOES.COM.BR/lote/casa-em-sorocaba/2405?origem=teste#dados"
                );


        assertEquals(
                "https://www.sublimeleiloes.com.br/lote/casa-em-sorocaba/2405/",
                normalizada
        );
    }
}
