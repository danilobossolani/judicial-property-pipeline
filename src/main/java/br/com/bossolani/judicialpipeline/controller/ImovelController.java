package br.com.bossolani.judicialpipeline.controller;

import br.com.bossolani.judicialpipeline.model.Imovel;
import br.com.bossolani.judicialpipeline.service.ImovelService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/imoveis")
public class ImovelController {

    private final ImovelService imovelService;

    public ImovelController(ImovelService imovelService) {
        this.imovelService = imovelService;
    }

    @GetMapping
    public List<Imovel> listar() {
        return imovelService.listarTodos();
    }
}