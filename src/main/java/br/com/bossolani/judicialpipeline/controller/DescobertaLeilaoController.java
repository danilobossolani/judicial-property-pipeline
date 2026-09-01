package br.com.bossolani.judicialpipeline.controller;

import br.com.bossolani.judicialpipeline.dto.ResultadoDescobertaDTO;
import br.com.bossolani.judicialpipeline.model.OrigemExecucaoDescoberta;
import br.com.bossolani.judicialpipeline.service.DescobertaAutomaticaService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/descobertas")
public class DescobertaLeilaoController {

    private final DescobertaAutomaticaService descobertaAutomaticaService;

    public DescobertaLeilaoController(
            DescobertaAutomaticaService descobertaAutomaticaService
    ) {

        this.descobertaAutomaticaService =
                descobertaAutomaticaService;
    }

    @PostMapping({"", "/sublime"})
    public ResultadoDescobertaDTO descobrirLotes() {

        return descobertaAutomaticaService
                .executarDescoberta(
                        OrigemExecucaoDescoberta.MANUAL
                );
    }
}
