package br.com.bossolani.judicialpipeline.controller;

import br.com.bossolani.judicialpipeline.dto.CentralAuditoriaDTO;
import br.com.bossolani.judicialpipeline.service.AuditoriaService;
import br.com.bossolani.judicialpipeline.service.DescobertaAutomaticaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuditoriaController {

    private final AuditoriaService auditoriaService;
    private final DescobertaAutomaticaService descobertaAutomaticaService;

    public AuditoriaController(
            AuditoriaService auditoriaService,
            DescobertaAutomaticaService descobertaAutomaticaService
    ) {

        this.auditoriaService =
                auditoriaService;

        this.descobertaAutomaticaService =
                descobertaAutomaticaService;
    }

    @GetMapping("/auditoria")
    public String central(
            Model model
    ) {

        CentralAuditoriaDTO central =
                auditoriaService.carregarCentral();


        model.addAttribute(
                "central",
                central
        );

        model.addAttribute(
                "descobertaEmExecucao",
                descobertaAutomaticaService.estaEmExecucao()
        );


        return "auditoria";
    }
}
