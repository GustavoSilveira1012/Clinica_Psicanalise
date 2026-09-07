package com.psicogest.psicogest.security.mfa;

import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

@Component
public class MfaSecretCipher {
    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public MfaSecretCipher(MfaProperties properties) {
        byte[] decoded;
        try { decoded = Base64.getDecoder().decode(properties.encryptionKey()); }
        catch (IllegalArgumentException e) { throw new IllegalStateException("MFA_ENCRYPTION_KEY deve ser Base64 de 32 bytes"); }
        if (decoded.length != 32) throw new IllegalStateException("MFA_ENCRYPTION_KEY deve conter 32 bytes");
        key = new SecretKeySpec(decoded, "AES");
    }

    public EncryptedSecret encrypt(String secret) {
        byte[] iv = new byte[12];
        random.nextBytes(iv);
        return new EncryptedSecret(Base64.getEncoder().encodeToString(
                crypt(Cipher.ENCRYPT_MODE, secret.getBytes(StandardCharsets.UTF_8), iv)),
                Base64.getEncoder().encodeToString(iv));
    }

    public String decrypt(String ciphertext, String iv) {
        return new String(crypt(Cipher.DECRYPT_MODE, Base64.getDecoder().decode(ciphertext),
                Base64.getDecoder().decode(iv)), StandardCharsets.UTF_8);
    }

    private byte[] crypt(int mode, byte[] value, byte[] iv) {
        if (iv.length != 12) throw new IllegalArgumentException("IV MFA inválido");
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(mode, key, new GCMParameterSpec(128, iv));
            return cipher.doFinal(value);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível processar o segredo MFA", e);
        }
    }

    public record EncryptedSecret(String ciphertext, String iv) {}
}
