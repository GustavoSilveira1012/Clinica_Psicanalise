package com.psicogest.psicogest.infrastructure.storage;

import java.util.List;
import java.util.UUID;

/** Tenant-scoped persistence boundary for the export retention worker. */
public interface ClinicalExportRetentionStore {

    List<ExpiredClinicalExport> claimExpired(UUID organizationId, int batchSize);

    void markExpired(UUID organizationId, ExpiredClinicalExport export);

    record ExpiredClinicalExport(UUID id, String storageKey) { }
}
