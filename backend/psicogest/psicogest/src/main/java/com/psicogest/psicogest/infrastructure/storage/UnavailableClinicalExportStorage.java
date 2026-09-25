package com.psicogest.psicogest.infrastructure.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.UUID;

/**
 * Fail-closed production adapter used until a durable private object store is
 * configured. It deliberately never writes clinical exports to the container's
 * local filesystem.
 */
@Component
@Profile("production")
@ConditionalOnProperty(
        name = "app.export-storage.type",
        havingValue = "disabled",
        matchIfMissing = true
)
public class UnavailableClinicalExportStorage implements SecureClinicalExportStorage {

    private IllegalStateException unavailable() {
        return new IllegalStateException("Armazenamento seguro de exportações clínicas não configurado");
    }

    @Override
    public StoredExport store(UUID financialEntityId, UUID exportId, byte[] content, String contentType) {
        throw unavailable();
    }

    @Override
    public InputStream open(UUID financialEntityId, String storageKey) {
        throw unavailable();
    }

    @Override
    public void delete(UUID financialEntityId, String storageKey) {
        throw unavailable();
    }
}
