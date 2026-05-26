package com.mekylei.transactionprocessing.configuracao.seguranca;

import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] ENDPOINTS_PUBLICOS = {
            "/actuator/health",
            "/actuator/info",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    private final ApiAutenticacaoEntryPoint autenticacaoEntryPoint;
    private final ApiAcessoNegadoHandler acessoNegadoHandler;
    private final JwtClaimsConverter jwtClaimsConverter;

    public SecurityConfig(ApiAutenticacaoEntryPoint autenticacaoEntryPoint,
                          ApiAcessoNegadoHandler acessoNegadoHandler,
                          JwtClaimsConverter jwtClaimsConverter) {
        this.autenticacaoEntryPoint = autenticacaoEntryPoint;
        this.acessoNegadoHandler = acessoNegadoHandler;
        this.jwtClaimsConverter = jwtClaimsConverter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(@NonNull HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(ENDPOINTS_PUBLICOS)
                        .permitAll()
                        .anyRequest()
                        .authenticated())

                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt ->
                                jwt.jwtAuthenticationConverter(jwtClaimsConverter))
                        .authenticationEntryPoint(autenticacaoEntryPoint))

                .exceptionHandling(exception ->
                        exception.accessDeniedHandler(acessoNegadoHandler));

        return http.build();
    }
}
