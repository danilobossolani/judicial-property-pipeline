package br.com.bossolani.judicialpipeline.controller;

import br.com.bossolani.judicialpipeline.model.Leilao;
import br.com.bossolani.judicialpipeline.service.LeilaoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leiloes")
public class LeilaoController {

    private final LeilaoService leilaoService;

    public LeilaoController(LeilaoService leilaoService) {
        this.leilaoService = leilaoService;
    }

    @GetMapping
    public List<Leilao> listar() {
        return leilaoService.listarTodos();
    }

    @PostMapping
    public Leilao salvar(@RequestBody Leilao leilao) {
        return leilaoService.salvar(leilao);
    }
}