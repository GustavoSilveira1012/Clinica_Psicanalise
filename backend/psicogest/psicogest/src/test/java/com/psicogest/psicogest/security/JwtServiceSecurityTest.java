package com.psicogest.psicogest.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.psicogest.psicogest.model.entity.User;
import com.psicogest.psicogest.model.enums.UserRole;
import com.psicogest.psicogest.security.jwt.JwtProperties;
import com.psicogest.psicogest.security.jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceSecurityTest {

    private JwtService service;
    private JwtDecoder decoder;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(keyPair.getPrivate())
                .keyID("test")
                .build();
        JWKSource<SecurityContext> source = new ImmutableJWKSet<>(
                new JWKSet(rsaKey));

        JwtProperties properties = new JwtProperties(
                "psicogest-api",
                "psicogest-web",
                Duration.ofMinutes(10),
                Duration.ofDays(14),
                null,
                null,
                "test",
                "psicogest_rt",
                false);

        service = new JwtService(new NimbusJwtEncoder(source), properties);
        decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
    }

    @Test
    void shouldIssueAnRs256AccessTokenWithOnlySecurityClaims() {
        User user = new User();
        user.setId(15L);
        user.setRole(UserRole.PSYCHOANALYST);
        user.setSecurityVersion(4);

        JwtService.AccessToken issued = service.issueAccessToken(user);
        var decoded = decoder.decode(issued.value());

        assertThat(JwtValidators.createDefaultWithIssuer("psicogest-api")
                .validate(decoded).hasErrors()).isFalse();
        assertThat(decoded.getClaims().get("iss")).isEqualTo("psicogest-api");
        assertThat(decoded.getAudience()).containsExactly("psicogest-web");
        assertThat(decoded.getSubject()).isEqualTo("15");
        assertThat(decoded.getClaimAsString("token_type")).isEqualTo("access");
        assertThat(decoded.getClaimAsStringList("roles"))
                .containsExactly("PSYCHOANALYST");
        assertThat(((Number) decoded.getClaim("sv")).intValue()).isEqualTo(4);
        assertThat(decoded.getClaims()).doesNotContainKeys(
                "patientName", "cpf", "clinicData", "medicalInformation");
    }

    @Test
    void shouldRejectAnAccessTokenWithAnInvalidSignature() {
        User user = new User();
        user.setId(15L);
        user.setRole(UserRole.PATIENT);
        user.setSecurityVersion(1);

        String token = service.issueAccessToken(user).value();
        char last = token.charAt(token.length() - 1);
        String tampered = token.substring(0, token.length() - 1)
                + (last == 'a' ? 'b' : 'a');

        assertThatThrownBy(() -> decoder.decode(tampered))
                .isInstanceOf(JwtException.class);
    }
}
