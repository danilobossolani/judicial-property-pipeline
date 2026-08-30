package br.com.bossolani.judicialpipeline.controller;

import br.com.bossolani.judicialpipeline.dto.DetalheImovelDTO;
import br.com.bossolani.judicialpipeline.service.DetalheImovelService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class DetalheImovelController {

    private final DetalheImovelService detalheImovelService;


    public DetalheImovelController(
            DetalheImovelService detalheImovelService
    ) {

        this.detalheImovelService =
                detalheImovelService;
    }


    @GetMapping("/imoveis/{id}")
    public String detalhe(
            @PathVariable Long id,
            Model model
    ) {

        DetalheImovelDTO detalhe =
                detalheImovelService
                        .buscarPorId(id);


        model.addAttribute(
                "detalhe",
                detalhe
        );


        return "imovel-detalhes";
    }
}