package com.psicogest.psicogest.security.refresh;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import java.util.Base64;
import java.util.HexFormat;

@Component
public class RefreshTokenGenerator {

    private static final SecureRandom RANDOM =
            new SecureRandom();

    public String generate() {

        byte[] bytes =
                new byte[32];

        RANDOM.nextBytes(bytes);

        return Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    public String hash(
            String token
    ) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] hashed =
                    digest.digest(
                            token.getBytes(
                                    java.nio.charset.StandardCharsets.UTF_8
                            )
                    );

            return HexFormat.of()
                    .formatHex(hashed);

        } catch (
                NoSuchAlgorithmException exception
        ) {

            throw new IllegalStateException(
                    exception
            );
        }
    }
}