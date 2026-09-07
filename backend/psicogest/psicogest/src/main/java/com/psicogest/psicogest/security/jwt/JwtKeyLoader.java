package com.psicogest.psicogest.security.jwt;

import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Component
public class JwtKeyLoader {

    private final JwtProperties properties;

    public JwtKeyLoader(
            JwtProperties properties
    ) {
        this.properties = properties;
    }

    public RSAPublicKey publicKey() {

        try (
                var input =
                        properties.publicKey()
                                .getInputStream()
        ) {

            return RsaKeyConverters
                    .x509()
                    .convert(input);

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Não foi possível carregar a chave pública JWT",
                    exception
            );
        }
    }

    public RSAPrivateKey privateKey() {

        try (
                var input =
                        properties.privateKey()
                                .getInputStream()
        ) {

            return RsaKeyConverters
                    .pkcs8()
                    .convert(input);

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Não foi possível carregar a chave privada JWT",
                    exception
            );
        }
    }
}