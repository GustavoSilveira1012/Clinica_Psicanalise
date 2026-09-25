package com.psicogest.psicogest.infrastructure.storage;

import java.io.InputStream;

/**
 * Provider boundary for a private, durable object store. Implementations must
 * use provider credentials from the deployment secret manager, never public
 * object URLs, and must verify (not assume) their bucket security settings.
 */
public interface PrivateObjectStorage {

    void put(String key, byte[] ciphertext, String contentType);

    InputStream get(String key);

    void delete(String key);

    /**
     * Must perform a synthetic write/read/delete probe and verify private
     * access, at-rest encryption, and the configured retention policy.
     */
    PrivateObjectStorageHealth healthCheck();
}
