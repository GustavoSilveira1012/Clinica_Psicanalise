package com.psicogest.psicogest.security.auth.jwt;

import com.psicogest.psicogest.model.entity.User;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

@Service
public class JwtService {
    private final JwtEncoder encoder;
    private final JwtProperties properties;
    public JwtService(JwtEncoder encoder, JwtProperties properties) {
        this.encoder = encoder; this.properties = properties;
    }
    public AccessToken issueAccessToken(User user, UUID sessionId) {
        Objects.requireNonNull(sessionId, "Sessão obrigatória para emitir JWT");
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.accessTokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(properties.issuer())
                .audience(List.of(properties.audience())).subject(user.getId().toString())
                .issuedAt(now).expiresAt(expiresAt).id(UUID.randomUUID().toString())
                .claim("roles", List.of(user.getRole().name())).claim("sv", user.getSecurityVersion())
                .claim("sid", sessionId.toString()).claim("token_type", "access").build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(properties.keyId()).type("JWT").build();
        Jwt jwt = encoder.encode(JwtEncoderParameters.from(header, claims));
        return new AccessToken(jwt.getTokenValue(), expiresAt);
    }
    public record AccessToken(String value, Instant expiresAt) {}
}
