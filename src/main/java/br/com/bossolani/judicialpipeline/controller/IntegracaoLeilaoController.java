package br.com.bossolani.judicialpipeline.controller;

import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteEnriquecidoDTO;
import br.com.bossolani.judicialpipeline.service.IntegracaoLeilaoService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/integracao")
public class IntegracaoLeilaoController {

    private final IntegracaoLeilaoService integracaoLeilaoService;

    public IntegracaoLeilaoController(
            IntegracaoLeilaoService integracaoLeilaoService
    ) {
        this.integracaoLeilaoService = integracaoLeilaoService;
    }

    @GetMapping({"", "/sublime"})
    public LoteEnriquecidoDTO buscarLote(
            @RequestParam String url
    ) throws IOException {

        return integracaoLeilaoService.buscarLote(url);
    }
}
