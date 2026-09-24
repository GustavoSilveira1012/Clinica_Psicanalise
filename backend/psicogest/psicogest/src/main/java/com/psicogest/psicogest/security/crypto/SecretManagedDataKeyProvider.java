package com.psicogest.psicogest.security.crypto;

import java.util.Arrays;
import java.util.Base64;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** AES envelope keys injected by the deployment secret manager. No generated fallback keys. */
@Component
@Profile("production")
@ConditionalOnProperty(name = "app.security.crypto.provider", havingValue = "secret-managed")
public class SecretManagedDataKeyProvider implements DataKeyProvider {
    private final LocalDevelopmentDataKeyProvider envelope;

    public SecretManagedDataKeyProvider(CryptoProperties properties) {
        if (properties.currentKeyId() == null || properties.currentKeyId().isBlank()
                || properties.localKeys() == null
                || !properties.localKeys().containsKey(properties.currentKeyId())) {
            throw new IllegalStateException("Chave de criptografia de produção não configurada");
        }
        properties.localKeys().values().forEach(encoded -> {
            byte[] key;
            try {
                key = Base64.getDecoder().decode(encoded);
            } catch (IllegalArgumentException exception) {
                throw new IllegalStateException("Chave de criptografia inválida");
            }
            try {
                if (key.length != 32 || Arrays.equals(key, new byte[32])) {
                    throw new IllegalStateException("Chave de criptografia deve conter 256 bits aleatórios");
                }
            } finally {
                Arrays.fill(key, (byte) 0);
            }
        });
        // Reuse the versioned envelope format so existing data and rotated keys remain readable.
        this.envelope = new LocalDevelopmentDataKeyProvider(properties);
    }

    @Override
    public GeneratedDataKey generateDataKey(EncryptionContext context) {
        return envelope.generateDataKey(context);
    }

    @Override
    public byte[] decryptDataKey(String keyId, byte[] wrappedDataKey, EncryptionContext context) {
        return envelope.decryptDataKey(keyId, wrappedDataKey, context);
    }
}
