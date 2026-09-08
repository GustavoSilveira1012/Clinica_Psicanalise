package com.psicogest.psicogest.security.crypto;

import java.util.Objects;

public record EncryptedEnvelope(
        int cryptoVersion,
        String algorithm,
        String keyId,
        byte[] wrappedDataKey,
        byte[] iv,
        byte[] ciphertext
) {
    public EncryptedEnvelope {
        Objects.requireNonNull( algorithm );
        Objects.requireNonNull( keyId );
        Objects.requireNonNull( wrappedDataKey );
        Objects.requireNonNull( iv );
        Objects.requireNonNull( ciphertext );
        wrappedDataKey = wrappedDataKey.clone();
        iv = iv.clone();
        ciphertext = ciphertext.clone();
    }

    @Override
    public byte[] wrappedDataKey() {
        return wrappedDataKey.clone();
    }

    @Override
    public byte[] iv() {
        return iv.clone();
    }

    @Override
    public byte[] ciphertext() {
        return ciphertext.clone();
    }
}
