package com.psicogest.psicogest.integration;

import com.psicogest.psicogest.security.auth.jwt.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.time.Instant;
import java.util.List;

@TestConfiguration(proxyBeanMethods = false)
@EnableConfigurationProperties(JwtProperties.class)
public class TestJwtConfiguration {

    @Bean
    JwtEncoder testJwtEncoder() {
        return parameters -> testJwt("test-token");
    }

    @Bean
    JwtDecoder testJwtDecoder() {
        return token -> testJwt(token);
    }

    @Bean
    JwtAuthenticationConverter testJwtAuthenticationConverter() {
        return new JwtAuthenticationConverter();
    }

    private Jwt testJwt(String token) {
        Instant issuedAt = Instant.now();
        return Jwt.withTokenValue(token)
                .header("alg", "none")
                .issuer("psicogest-api")
                .audience(List.of("psicogest-web"))
                .subject("test-user")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(600))
                .build();
    }
}
