package br.com.bossolani.judicialpipeline.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record ConfiguracaoSegurancaProperties(
        boolean enabled,
        String username,
        String password
) {
}
