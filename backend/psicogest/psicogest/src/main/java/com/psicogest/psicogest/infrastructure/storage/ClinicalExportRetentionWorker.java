package com.psicogest.psicogest.infrastructure.storage;

import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import com.psicogest.psicogest.exception.AccessDeniedException;
import com.psicogest.psicogest.security.tenant.TenantContext;
import com.psicogest.psicogest.security.tenant.TenantContextHolder;

/**
 * Deletes expired export objects only for the tenant already selected by a
 * trusted per-tenant job. No global tenant enumeration or public endpoint.
 * EXPIRING is a durable claim; if deletion or DB finalization fails, a later
 * run safely retries the idempotent object delete.
 */
@Component
@Profile("production")
@ConditionalOnProperty(name = "app.export-storage.type", havingValue = "supabase-s3")
@ConditionalOnProperty(name = "app.export-storage.retention.enabled", havingValue = "true")
public class ClinicalExportRetentionWorker {

    private static final int MAX_BATCH_SIZE = 100;

    private final ClinicalExportRetentionStore retentionStore;
    private final SecureClinicalExportStorage storage;
    private final int batchSize;

    public ClinicalExportRetentionWorker(
            ClinicalExportRetentionStore retentionStore,
            SecureClinicalExportStorage storage,
            @Value("${app.export-storage.retention.batch-size:25}") int batchSize
    ) {
        if (batchSize < 1 || batchSize > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("Lote de retenção inválido");
        }
        this.retentionStore = retentionStore;
        this.storage = storage;
        this.batchSize = batchSize;
    }

    /**
     * Processes a bounded tenant batch. Failed items remain EXPIRING and are
     * eligible for retry; provider details and object keys are never returned.
     */
    public RetentionBatchResult processCurrentTenantBatch() {
        TenantContext tenant = TenantContextHolder.get();
        if (tenant == null || tenant.organizationId() == null) {
            throw new AccessDeniedException("Contexto de organização obrigatório para expirar exports");
        }
        List<ClinicalExportRetentionStore.ExpiredClinicalExport> expired =
                retentionStore.claimExpired(tenant.organizationId(), batchSize);
        int finalized = 0;
        int retryPending = 0;
        for (ClinicalExportRetentionStore.ExpiredClinicalExport export : expired) {
            try {
                storage.delete(tenant.organizationId(), export.storageKey());
                retentionStore.markExpired(tenant.organizationId(), export);
                finalized++;
            } catch (RuntimeException exception) {
                // Keep the durable claim for a later retry without logging the
                // exception: providers may include object keys or credentials.
                retryPending++;
            }
        }
        return new RetentionBatchResult(expired.size(), finalized, retryPending);
    }

    public record RetentionBatchResult(int selected, int finalized, int retryPending) { }
}
