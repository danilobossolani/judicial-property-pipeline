package br.com.bossolani.judicialpipeline.controller;

import br.com.bossolani.judicialpipeline.dto.PipelineImovelDTO;
import br.com.bossolani.judicialpipeline.model.ResultadoLeilao;
import br.com.bossolani.judicialpipeline.model.StatusPipeline;
import br.com.bossolani.judicialpipeline.service.PainelService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class PainelController {

    private final PainelService painelService;


    public PainelController(
            PainelService painelService
    ) {

        this.painelService =
                painelService;
    }


    @GetMapping("/")
    public String painel(
            Model model
    ) {

        List<PipelineImovelDTO> imoveis =
                painelService.listarImoveis();


        long totalImoveis =
                imoveis.size();


        /*
         * Consideramos "em acompanhamento" qualquer imóvel
         * que ainda faça parte do pipeline operacional.
         *
         * DESCARTADO e ENCERRADO deixam de contar.
         */
        long totalMonitorando =
                imoveis
                        .stream()
                        .filter(imovel ->
                                imovel.statusPipeline()
                                        != StatusPipeline.DESCARTADO
                        )
                        .filter(imovel ->
                                imovel.statusPipeline()
                                        != StatusPipeline.ENCERRADO
                        )
                        .count();


        long totalSemLances =
                imoveis
                        .stream()
                        .filter(imovel ->
                                imovel.resultadoLeilao()
                                        == ResultadoLeilao.SEM_LANCES
                        )
                        .count();


        long totalOportunidades =
                imoveis
                        .stream()
                        .filter(imovel ->
                                imovel.statusPipeline()
                                        == StatusPipeline.OPORTUNIDADE
                        )
                        .count();


        model.addAttribute(
                "imoveis",
                imoveis
        );


        model.addAttribute(
                "totalImoveis",
                totalImoveis
        );


        model.addAttribute(
                "totalMonitorando",
                totalMonitorando
        );


        model.addAttribute(
                "totalSemLances",
                totalSemLances
        );


        model.addAttribute(
                "totalOportunidades",
                totalOportunidades
        );


        return "painel";
    }
}
