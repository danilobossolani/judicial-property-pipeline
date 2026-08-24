package br.com.bossolani.judicialpipeline.controller;

import br.com.bossolani.judicialpipeline.model.Fonte;
import br.com.bossolani.judicialpipeline.service.FonteService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fontes")
public class FonteController {

    private final FonteService fonteService;

    public FonteController(FonteService fonteService) {
        this.fonteService = fonteService;
    }

    @GetMapping
    public List<Fonte> listar() {
        return fonteService.listarTodos();
    }

    @PostMapping
    public Fonte salvar(@RequestBody Fonte fonte) {
        return fonteService.salvar(fonte);
    }
}