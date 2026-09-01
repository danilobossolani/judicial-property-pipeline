package br.com.bossolani.judicialpipeline.controller;

import br.com.bossolani.judicialpipeline.model.Fonte;
import br.com.bossolani.judicialpipeline.service.PersistenciaLeilaoService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coletas")
public class PersistenciaLeilaoController {

    private final PersistenciaLeilaoService persistenciaLeilaoService;

    public PersistenciaLeilaoController(
            PersistenciaLeilaoService persistenciaLeilaoService
    ) {
        this.persistenciaLeilaoService = persistenciaLeilaoService;
    }

    @PostMapping({"", "/sublime"})
    public Fonte coletarESalvar(
            @RequestParam String url
    ) throws Exception {

        return persistenciaLeilaoService.coletarESalvar(url);
    }
}
