package br.com.bossolani.judicialpipeline.dto;

import br.com.bossolani.judicialpipeline.model.FonteTipo;
import br.com.bossolani.judicialpipeline.model.ResultadoLeilao;
import br.com.bossolani.judicialpipeline.model.StatusLeilao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DetalheImovelDTOTest {

    @Test
    void deveSinalizarDivergenciaSemEscolherUmaFonteComoVerdade() {

        DetalheImovelDTO.FonteResumo sublime =
                fonte(
                        "Sublime Leilões",
                        new BigDecimal("200000.00"),
                        new BigDecimal("120000.00")
                );
        DetalheImovelDTO.FonteResumo mega =
                fonte(
                        "Mega Leilões",
                        new BigDecimal("210000.00"),
                        new BigDecimal("126000.00")
                );

        DetalheImovelDTO detalhe =
                new DetalheImovelDTO(
                        null,
                        null,
                        null,
                        null,
                        List.of(),
                        List.of(sublime, mega)
                );

        assertTrue(detalhe.possuiMultiplasFontes());
        assertTrue(detalhe.possuiDivergenciaEntreFontes());
    }

    private DetalheImovelDTO.FonteResumo fonte(
            String nome,
            BigDecimal avaliacao,
            BigDecimal segundaPraca
    ) {

        return new DetalheImovelDTO.FonteResumo(
                null,
                FonteTipo.LEILOEIRO_OFICIAL,
                nome,
                "https://example.invalid/lote",
                LocalDateTime.of(2026, 8, 31, 12, 0),
                avaliacao,
                avaliacao,
                segundaPraca,
                segundaPraca,
                StatusLeilao.ENCERRADO,
                ResultadoLeilao.SEM_LANCES
        );
    }
}
