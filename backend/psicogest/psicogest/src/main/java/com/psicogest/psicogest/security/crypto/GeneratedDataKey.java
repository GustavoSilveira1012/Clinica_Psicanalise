package com.psicogest.psicogest.security.crypto;

import java.util.Arrays;

public final class GeneratedDataKey implements AutoCloseable {

    private final byte[] plaintextKey;
    private final byte[] wrappedKey;
    private final String keyId;

    public GeneratedDataKey(
            byte[] plaintextKey,
            byte[] wrappedKey,
            String keyId
    ) {
        this.plaintextKey = plaintextKey.clone();
        this.wrappedKey = wrappedKey.clone();
        this.keyId = keyId;
    }

    public byte[] plaintextKey() {
        return plaintextKey;
    }

    public byte[] wrappedKey() {
        return wrappedKey.clone();
    }

    public String keyId() {
        return keyId;
    }

    @Override
    public void close() {
        Arrays.fill( plaintextKey, (byte) 0 );
    }
}
