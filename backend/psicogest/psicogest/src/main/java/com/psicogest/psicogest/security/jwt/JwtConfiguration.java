package com.psicogest.psicogest.security.jwt;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;

import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;

import com.nimbusds.jose.proc.SecurityContext;

import org.springframework.boot.context.properties.EnableConfigurationProperties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

@Configuration
@EnableConfigurationProperties(
        JwtProperties.class
)
public class JwtConfiguration {

    @Bean
    public JwtEncoder jwtEncoder(
            JwtKeyLoader keyLoader,
            JwtProperties properties
    ) {

        RSAKey rsaKey =
                new RSAKey.Builder(
                        keyLoader.publicKey()
                )
                        .privateKey(
                                keyLoader.privateKey()
                        )
                        .keyID(
                                properties.keyId()
                        )
                        .build();

        JWKSource<SecurityContext> source =
                new ImmutableJWKSet<>(
                        new JWKSet(rsaKey)
                );

        return new NimbusJwtEncoder(
                source
        );
    }

    @Bean
    public JwtDecoder jwtDecoder(
            JwtKeyLoader keyLoader,
            JwtProperties properties,
            AccountStateJwtValidator accountValidator
    ) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withPublicKey(keyLoader.publicKey())
                .build();

        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators
                .createDefaultWithIssuer(properties.issuer());

        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(
                properties.audience());

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                issuerValidator,
                audienceValidator,
                accountValidator));

        return decoder;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities =
                new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
