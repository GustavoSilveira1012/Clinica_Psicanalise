package com.psicogest.psicogest.security.auth.jwt;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.junit.jupiter.api.Test;

class JwtKeyPairValidatorTest {
    @Test
    void acceptsMatchingRsaKeys() throws Exception {
        KeyPair pair = generate(2048);
        assertDoesNotThrow(() -> JwtKeyPairValidator.validate(
                (RSAPublicKey) pair.getPublic(), (RSAPrivateKey) pair.getPrivate()));
    }

    @Test
    void rejectsMismatchedRsaKeys() throws Exception {
        KeyPair first = generate(2048);
        KeyPair second = generate(2048);
        assertThrows(IllegalStateException.class, () -> JwtKeyPairValidator.validate(
                (RSAPublicKey) first.getPublic(), (RSAPrivateKey) second.getPrivate()));
    }

    @Test
    void rejectsWeakRsaKeys() throws Exception {
        KeyPair pair = generate(1024);
        assertThrows(IllegalStateException.class, () -> JwtKeyPairValidator.validate(
                (RSAPublicKey) pair.getPublic(), (RSAPrivateKey) pair.getPrivate()));
    }

    private KeyPair generate(int bits) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(bits);
        return generator.generateKeyPair();
    }
}
