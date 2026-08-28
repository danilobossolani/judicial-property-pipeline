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
        this.painelService = painelService;
    }

    @GetMapping("/")
    public String painel(Model model) {

        List<PipelineImovelDTO> imoveis =
                painelService.listarImoveis();

        long monitorando =
                imoveis.stream()
                        .filter(imovel ->
                                imovel.statusPipeline()
                                        == StatusPipeline.MONITORANDO_LEILAO
                                        ||
                                        imovel.statusPipeline()
                                                == StatusPipeline.MONITORANDO_PROCESSO
                        )
                        .count();

        long semLances =
                imoveis.stream()
                        .filter(imovel ->
                                imovel.resultadoLeilao()
                                        == ResultadoLeilao.SEM_LANCES
                        )
                        .count();

        model.addAttribute(
                "imoveis",
                imoveis
        );

        model.addAttribute(
                "totalImoveis",
                imoveis.size()
        );

        model.addAttribute(
                "totalMonitorando",
                monitorando
        );

        model.addAttribute(
                "totalSemLances",
                semLances
        );

        return "painel";
    }
}