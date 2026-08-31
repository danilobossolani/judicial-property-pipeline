package br.com.bossolani.judicialpipeline.controller;

import br.com.bossolani.judicialpipeline.model.StatusPipeline;
import br.com.bossolani.judicialpipeline.service.AcompanhamentoOperacionalService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AcompanhamentoController {

    private final AcompanhamentoOperacionalService acompanhamentoOperacionalService;


    public AcompanhamentoController(
            AcompanhamentoOperacionalService acompanhamentoOperacionalService
    ) {

        this.acompanhamentoOperacionalService =
                acompanhamentoOperacionalService;
    }


    @PostMapping("/imoveis/{id}/acompanhamento")
    public String atualizarAcompanhamento(
            @PathVariable Long id,

            @RequestParam
            StatusPipeline statusPipeline,

            @RequestParam(required = false)
            String observacao,

            RedirectAttributes redirectAttributes
    ) {

        acompanhamentoOperacionalService.atualizar(
                id,
                statusPipeline,
                observacao
        );


        redirectAttributes.addFlashAttribute(
                "mensagemSucesso",
                "Acompanhamento atualizado com sucesso."
        );


        return "redirect:/imoveis/" + id;
    }
}