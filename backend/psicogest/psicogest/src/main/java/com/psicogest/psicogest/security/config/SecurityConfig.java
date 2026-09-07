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

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.RequestMatcher;

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
    public CorsConfigurationSource corsConfigurationSource(
            SecurityProperties properties
    ) {

        if (properties.allowedOrigins() == null
                || properties.allowedOrigins().stream().anyMatch("*"::equals)) {
            throw new IllegalStateException(
                    "CORS com credenciais exige origens explícitas");
        }

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
                true
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
            RestAccessDeniedHandler accessDeniedHandler,
            JwtAuthenticationConverter jwtAuthenticationConverter
    ) throws Exception {

        RequestMatcher authCsrfMatcher = request -> {
            if (!"POST".equalsIgnoreCase(request.getMethod())) {
                return false;
            }

            String uri = request.getRequestURI();
            return uri.equals("/auth/login")
                    || uri.equals("/auth/refresh")
                    || uri.equals("/auth/logout")
                    || uri.equals("/auth/logout-all");
        };

        CookieCsrfTokenRepository csrfRepository =
                CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookieName("psicogest_csrf");
        csrfRepository.setHeaderName("X-CSRF-TOKEN");

        http

                .csrf(
                        csrf -> csrf
                                .csrfTokenRepository(csrfRepository)
                                .requireCsrfProtectionMatcher(authCsrfMatcher)
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

                                 .requestMatchers(
                                         "/auth/login",
                                         "/auth/refresh",
                                         "/auth/csrf"
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

                .oauth2ResourceServer(
                        oauth -> oauth
                                .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                        jwtAuthenticationConverter))
                                .authenticationEntryPoint(authenticationEntryPoint)
                                .accessDeniedHandler(accessDeniedHandler)
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
