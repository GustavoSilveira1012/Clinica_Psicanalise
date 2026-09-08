package com.psicogest.psicogest.security.audit;

import javax.crypto.SecretKey;

public interface AuditKeyProvider {

    String currentKeyId();

    SecretKey currentKey();

    SecretKey keyFor(
            String keyId
    );
}
