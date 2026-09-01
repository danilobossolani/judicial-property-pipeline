package br.com.bossolani.judicialpipeline.controller;

import br.com.bossolani.judicialpipeline.config.ConfiguracaoSegurancaProperties;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

@ControllerAdvice
public class IdentidadeViewAdvice {

    private final ConfiguracaoSegurancaProperties properties;

    public IdentidadeViewAdvice(
            ConfiguracaoSegurancaProperties properties
    ) {
        this.properties = properties;
    }

    @ModelAttribute
    public void adicionarIdentidade(
            Model model,
            Principal principal
    ) {

        model.addAttribute(
                "segurancaHabilitada",
                properties.enabled()
        );

        model.addAttribute(
                "usuarioAtual",
                principal != null
                        ? principal.getName()
                        : null
        );
    }
}
