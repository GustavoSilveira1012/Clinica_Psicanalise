package com.psicogest.psicogest.security.crypto;

import java.util.Objects;
import java.util.Arrays;

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

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof EncryptedEnvelope that)) return false;
        return cryptoVersion == that.cryptoVersion
                && Objects.equals(algorithm, that.algorithm)
                && Objects.equals(keyId, that.keyId)
                && Arrays.equals(wrappedDataKey, that.wrappedDataKey)
                && Arrays.equals(iv, that.iv)
                && Arrays.equals(ciphertext, that.ciphertext);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(cryptoVersion, algorithm, keyId);
        result = 31 * result + Arrays.hashCode(wrappedDataKey);
        result = 31 * result + Arrays.hashCode(iv);
        result = 31 * result + Arrays.hashCode(ciphertext);
        return result;
    }
}
