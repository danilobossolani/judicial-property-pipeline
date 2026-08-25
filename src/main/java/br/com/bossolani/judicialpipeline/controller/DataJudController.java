package br.com.bossolani.judicialpipeline.controller;

import br.com.bossolani.judicialpipeline.integration.DataJudClient;
import br.com.bossolani.judicialpipeline.integration.datajud.dto.DataJudProcessoDTO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/datajud")
public class DataJudController {

    private final DataJudClient dataJudClient;

    public DataJudController(DataJudClient dataJudClient) {
        this.dataJudClient = dataJudClient;
    }

    @GetMapping("/processos/{numero}")
    public DataJudProcessoDTO buscarProcesso(
            @PathVariable String numero
    ) {
        return dataJudClient.buscarProcesso(numero);
    }
}