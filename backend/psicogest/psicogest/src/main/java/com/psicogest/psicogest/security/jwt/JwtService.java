package com.psicogest.psicogest.security.jwt;

import com.psicogest.psicogest.model.entity.User;

import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;

import org.springframework.security.oauth2.jwt.*;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;

    private final JwtProperties properties;

    public JwtService(
            JwtEncoder jwtEncoder,
            JwtProperties properties
    ) {

        this.jwtEncoder =
                jwtEncoder;

        this.properties =
                properties;
    }

    public AccessToken issueAccessToken(
            User user
    ) {

        Instant now =
                Instant.now();

        Instant expiresAt =
                now.plus(
                        properties.accessTokenTtl()
                );

        JwtClaimsSet claims =
                JwtClaimsSet.builder()

                        .issuer(
                                properties.issuer()
                        )

                        .audience(
                                List.of(
                                        properties.audience()
                                )
                        )

                        .subject(
                                user.getId().toString()
                        )

                        .issuedAt(now)

                        .expiresAt(
                                expiresAt
                        )

                        .id(
                                UUID.randomUUID()
                                        .toString()
                        )

                        .claim(
                                "roles",
                                List.of(
                                        user
                                                .getRole()
                                                .name()
                                )
                        )

                        .claim(
                                "sv",
                                user.getSecurityVersion()
                        )

                        .claim(
                                "token_type",
                                "access"
                        )

                        .build();

        JwsHeader header =
                JwsHeader
                        .with(
                                SignatureAlgorithm.RS256
                        )

                        .keyId(
                                properties.keyId()
                        )

                        .type("JWT")

                        .build();

        Jwt jwt =
                jwtEncoder.encode(
                        JwtEncoderParameters.from(
                                header,
                                claims
                        )
                );

        return new AccessToken(
                jwt.getTokenValue(),
                expiresAt
        );
    }

    public record AccessToken(

            String value,

            Instant expiresAt

    ) {
    }
}