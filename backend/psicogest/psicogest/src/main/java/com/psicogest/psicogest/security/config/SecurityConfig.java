package com.psicogest.psicogest.security.config;

import com.psicogest.psicogest.security.handler.RestAccessDeniedHandler;
import com.psicogest.psicogest.security.handler.RestAuthenticationEntryPoint;

import org.springframework.boot.context.properties.EnableConfigurationProperties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import org.springframework.security.config.Customizer;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(
        SecurityProperties.class
)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder(
                12
        );
    }

    @Bean
public CorsConfigurationSource corsConfigurationSource(
        SecurityProperties properties
) {

    CorsConfiguration configuration =
            new CorsConfiguration();

    configuration.setAllowedOrigins(
            properties.allowedOrigins()
    );

    configuration.setAllowedMethods(
            List.of(
                    "GET",
                    "POST",
                    "PUT",
                    "PATCH",
                    "DELETE",
                    "OPTIONS"
            )
    );

    configuration.setAllowedHeaders(
            List.of(
                    HttpHeaders.AUTHORIZATION,
                    HttpHeaders.CONTENT_TYPE,
                    "X-CSRF-TOKEN",
                    "Idempotency-Key"
            )
    );

    configuration.setAllowCredentials(
            false
    );

    configuration.setMaxAge(
            3600L
    );

    UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();

    source.registerCorsConfiguration(
            "/**",
            configuration
    );

    return source;
}

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler
    ) throws Exception {

        http

                /*
                 * API REST.
                 *
                 * Quando introduzirmos refresh token
                 * HttpOnly, vamos proteger especificamente
                 * os endpoints baseados em cookie contra CSRF.
                 */
                .csrf(
                        csrf ->
                                csrf.disable()
                )

                .cors(
                        Customizer.withDefaults()
                )

                .sessionManagement(
                        session ->
                                session.sessionCreationPolicy(
                                        SessionCreationPolicy.STATELESS
                                )
                )

                .exceptionHandling(
                        exceptions ->
                                exceptions

                                        .authenticationEntryPoint(
                                                authenticationEntryPoint
                                        )

                                        .accessDeniedHandler(
                                                accessDeniedHandler
                                        )
                )

                .authorizeHttpRequests(
                        auth -> auth

                                /*
                                 * Preflight CORS.
                                 */
                                .requestMatchers(
                                        HttpMethod.OPTIONS,
                                        "/**"
                                )
                                .permitAll()

                                /*
                                 * Login e refresh ainda
                                 * serão implementados.
                                 */
                                .requestMatchers(
                                        "/auth/login",
                                        "/auth/refresh"
                                )
                                .permitAll()

                                /*
                                 * Tudo que não foi
                                 * explicitamente liberado
                                 * exige autenticação.
                                 */
                                .anyRequest()
                                .authenticated()
                )

                .headers(
                        headers -> headers

                                .contentTypeOptions(
                                        Customizer.withDefaults()
                                )

                                .frameOptions(
                                        frame ->
                                                frame.deny()
                                )

                                .httpStrictTransportSecurity(
                                        hsts ->
                                                hsts
                                                        .includeSubDomains(
                                                                true
                                                        )
                                                        .maxAgeInSeconds(
                                                                31_536_000
                                                        )
                                )

                                .contentSecurityPolicy(
                                        csp ->
                                                csp.policyDirectives(
                                                        "default-src 'none'; "
                                                        + "frame-ancestors 'none'; "
                                                        + "base-uri 'none'"
                                                )
                                )
                );

        return http.build();
    }
}