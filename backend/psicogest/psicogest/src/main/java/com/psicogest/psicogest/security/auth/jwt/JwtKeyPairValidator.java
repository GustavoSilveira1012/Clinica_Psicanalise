package com.psicogest.psicogest.security.auth.jwt;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;

/** Reject a mismatched JWT PEM pair before serving any authentication traffic. */
final class JwtKeyPairValidator {
    private JwtKeyPairValidator() {
    }

    static void validate(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
        if (publicKey == null || privateKey == null || publicKey.getModulus().bitLength() < 2048) {
            throw new IllegalStateException("JWT key pair is missing or too weak");
        }
        byte[] challenge = new byte[32];
        new SecureRandom().nextBytes(challenge);
        try {
            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(privateKey);
            signer.update(challenge);
            byte[] signature = signer.sign();
            try {
                Signature verifier = Signature.getInstance("SHA256withRSA");
                verifier.initVerify(publicKey);
                verifier.update(challenge);
                if (!verifier.verify(signature)) {
                    throw new IllegalStateException("JWT public and private keys do not match");
                }
            } finally {
                Arrays.fill(signature, (byte) 0);
            }
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("JWT key pair validation failed", exception);
        } finally {
            Arrays.fill(challenge, (byte) 0);
        }
    }
}
