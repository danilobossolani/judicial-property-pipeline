package br.com.bossolani.judicialpipeline.service;

import br.com.bossolani.judicialpipeline.dto.PipelineImovelDTO;
import br.com.bossolani.judicialpipeline.model.Fonte;
import br.com.bossolani.judicialpipeline.model.Imovel;
import br.com.bossolani.judicialpipeline.model.Leilao;
import br.com.bossolani.judicialpipeline.model.Processo;
import br.com.bossolani.judicialpipeline.repository.AcompanhamentoRepository;
import br.com.bossolani.judicialpipeline.repository.FonteRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PainelServiceTest {

    @Test
    void deveOcultarDespejoEDeduplicarImovelPelaFonteMaisRecente() {

        FonteRepository fonteRepository =
                mock(
                        FonteRepository.class
                );

        AcompanhamentoRepository acompanhamentoRepository =
                mock(
                        AcompanhamentoRepository.class
                );


        Imovel imovelValido =
                imovel(
                        10L,
                        "Execução de Título Extrajudicial"
                );

        Imovel imovelDeDespejo =
                imovel(
                        11L,
                        "Despejo por Falta de Pagamento"
                );


        Fonte fonteAntiga =
                fonte(
                        imovelValido,
                        "https://www.sublimeleiloes.com.br/lote/casa-em-sorocaba/1000/",
                        LocalDateTime.of(
                                2026,
                                8,
                                30,
                                10,
                                0
                        )
                );

        Fonte fonteRecente =
                fonte(
                        imovelValido,
                        "https://www.sublimeleiloes.com.br/lote/casa-em-sorocaba/2000/",
                        LocalDateTime.of(
                                2026,
                                8,
                                31,
                                10,
                                0
                        )
                );

        Fonte fonteDespejo =
                fonte(
                        imovelDeDespejo,
                        "https://www.sublimeleiloes.com.br/lote/casa-em-sorocaba/3000/",
                        LocalDateTime.of(
                                2026,
                                8,
                                31,
                                11,
                                0
                        )
                );


        when(fonteRepository.findAll())
                .thenReturn(
                        List.of(
                                fonteAntiga,
                                fonteDespejo,
                                fonteRecente
                        )
                );

        when(acompanhamentoRepository.findByImovelId(
                anyLong()
        )).thenReturn(
                Optional.empty()
        );


        PainelService service =
                new PainelService(
                        fonteRepository,
                        acompanhamentoRepository
                );


        List<PipelineImovelDTO> resultado =
                service.listarImoveis();


        assertThat(resultado)
                .hasSize(1);

        assertThat(
                resultado.getFirst()
                        .imovelId()
        ).isEqualTo(
                10L
        );

        assertThat(
                resultado.getFirst()
                        .urlOrigem()
        ).isEqualTo(
                "https://www.sublimeleiloes.com.br/lote/casa-em-sorocaba/2000/"
        );
    }

    private Imovel imovel(
            Long id,
            String classeProcessual
    ) {

        Processo processo =
                mock(
                        Processo.class
                );

        Imovel imovel =
                mock(
                        Imovel.class
                );


        when(processo.getClasse())
                .thenReturn(
                        classeProcessual
                );

        when(processo.getNumeroProcesso())
                .thenReturn(
                        "00467199720118260602"
                );

        when(imovel.getId())
                .thenReturn(
                        id
                );

        when(imovel.getProcesso())
                .thenReturn(
                        processo
                );


        return imovel;
    }

    private Fonte fonte(
            Imovel imovel,
            String url,
            LocalDateTime dataCaptura
    ) {

        Leilao leilao =
                mock(
                        Leilao.class
                );

        Fonte fonte =
                mock(
                        Fonte.class
                );


        when(leilao.getImovel())
                .thenReturn(
                        imovel
                );

        when(fonte.getLeilao())
                .thenReturn(
                        leilao
                );

        when(fonte.getUrlOrigem())
                .thenReturn(
                        url
                );

        when(fonte.getDataCaptura())
                .thenReturn(
                        dataCaptura
                );


        return fonte;
    }
}
