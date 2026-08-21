package br.com.bossolani.judicialpipeline.controller;

import br.com.bossolani.judicialpipeline.model.Processo;
import br.com.bossolani.judicialpipeline.service.ProcessoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/processos")
public class ProcessoController {

    private final ProcessoService processoService;

    public ProcessoController(ProcessoService processoService) {
        this.processoService = processoService;
    }

    @GetMapping
    public List<Processo> listar() {
        return processoService.listarTodos();
    }

    @PostMapping
    public Processo salvar(@RequestBody Processo processo) {
        return processoService.salvar(processo);
    }
}