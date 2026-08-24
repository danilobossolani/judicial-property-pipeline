package br.com.bossolani.judicialpipeline.controller;

import br.com.bossolani.judicialpipeline.integration.DataJudClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/datajud")
public class DataJudController {

    private final DataJudClient dataJudClient;

    public DataJudController(DataJudClient dataJudClient) {
        this.dataJudClient = dataJudClient;
    }

    @GetMapping("/processos/{numero}")
    public String buscarProcesso(@PathVariable String numero) {
        return dataJudClient.buscarProcesso(numero);
    }
}