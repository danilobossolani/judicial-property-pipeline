package br.com.bossolani.judicialpipeline.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(
        ConfiguracaoSegurancaProperties.class
)
public class ConfiguracaoSeguranca {

    private static final int TAMANHO_MINIMO_SENHA = 12;

    @Bean
    @ConditionalOnProperty(
            name = "app.security.enabled",
            havingValue = "true"
    )
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @ConditionalOnProperty(
            name = "app.security.enabled",
            havingValue = "true"
    )
    public UserDetailsService userDetailsService(
            ConfiguracaoSegurancaProperties properties,
            PasswordEncoder passwordEncoder
    ) {

        validarCredenciais(properties);

        UserDetails operador =
                User.withUsername(properties.username().trim())
                        .password(
                                passwordEncoder.encode(
                                        properties.password()
                                )
                        )
                        .roles("ADMIN")
                        .build();

        return new InMemoryUserDetailsManager(operador);
    }

    @Bean
    @ConditionalOnProperty(
            name = "app.security.enabled",
            havingValue = "false",
            matchIfMissing = true
    )
    public UserDetailsService localUserDetailsService() {
        return new InMemoryUserDetailsManager();
    }

    @Bean
    @Order(1)
    @ConditionalOnProperty(
            name = "app.security.enabled",
            havingValue = "true"
    )
    public SecurityFilterChain apiEActuatorSecurityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .securityMatcher("/api/**", "/actuator/**")
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**"
                        ).permitAll()
                        .requestMatchers(
                                "/actuator/prometheus"
                        ).hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    @Order(2)
    @ConditionalOnProperty(
            name = "app.security.enabled",
            havingValue = "true"
    )
    public SecurityFilterChain interfaceSecurityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/login",
                                "/css/**",
                                "/js/**",
                                "/favicon.ico",
                                "/error"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", false)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                );

        return http.build();
    }

    @Bean
    @Order(1)
    @ConditionalOnProperty(
            name = "app.security.enabled",
            havingValue = "false",
            matchIfMissing = true
    )
    public SecurityFilterChain acessoLocalSecurityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().permitAll()
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    private void validarCredenciais(
            ConfiguracaoSegurancaProperties properties
    ) {

        if (properties.username() == null
                || properties.username().isBlank()) {
            throw new IllegalStateException(
                    "APP_SECURITY_USERNAME é obrigatório quando a segurança está habilitada."
            );
        }

        if (properties.password() == null
                || properties.password().length()
                < TAMANHO_MINIMO_SENHA) {
            throw new IllegalStateException(
                    "APP_SECURITY_PASSWORD deve ter pelo menos 12 caracteres quando a segurança está habilitada."
            );
        }
    }
}
