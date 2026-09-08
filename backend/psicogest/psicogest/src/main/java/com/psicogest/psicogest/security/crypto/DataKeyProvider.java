package com.psicogest.psicogest.security.crypto;

public interface DataKeyProvider {

    GeneratedDataKey generateDataKey(
            EncryptionContext context
    );

    byte[] decryptDataKey(
            String keyId,
            byte[] wrappedDataKey,
            EncryptionContext context
    );
}
