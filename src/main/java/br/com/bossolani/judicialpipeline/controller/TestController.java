package br.com.bossolani.judicialpipeline.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/api/teste")
    public String teste() {
        return "API funcionando";
    }
}