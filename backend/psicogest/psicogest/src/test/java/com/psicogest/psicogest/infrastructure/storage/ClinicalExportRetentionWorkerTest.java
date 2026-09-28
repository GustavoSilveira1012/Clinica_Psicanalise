package com.psicogest.psicogest.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.psicogest.psicogest.exception.AccessDeniedException;
import com.psicogest.psicogest.security.tenant.TenantContext;
import com.psicogest.psicogest.security.tenant.TenantContextHolder;

@ExtendWith(MockitoExtension.class)
class ClinicalExportRetentionWorkerTest {

    @Mock
    private ClinicalExportRetentionStore retentionStore;

    @Mock
    private SecureClinicalExportStorage storage;

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void deletesAndFinalizesOnlyForTheCurrentOrganization() {
        UUID organizationId = UUID.randomUUID();
        var export = new ClinicalExportRetentionStore.ExpiredClinicalExport(
                UUID.randomUUID(), organizationId + "/" + UUID.randomUUID());
        TenantContextHolder.set(new TenantContext(organizationId, 42L, null));
        when(retentionStore.claimExpired(organizationId, 20)).thenReturn(List.of(export));
        var worker = new ClinicalExportRetentionWorker(retentionStore, storage, 20);

        assertThat(worker.processCurrentTenantBatch()).isEqualTo(
                new ClinicalExportRetentionWorker.RetentionBatchResult(1, 1, 0));

        verify(storage).delete(organizationId, export.storageKey());
        verify(retentionStore).markExpired(organizationId, export);
    }

    @Test
    void requiresTrustedTenantContextBeforeClaimingAnyExport() {
        TenantContextHolder.clear();
        var worker = new ClinicalExportRetentionWorker(retentionStore, storage, 20);

        assertThatThrownBy(worker::processCurrentTenantBatch)
                .isInstanceOf(AccessDeniedException.class);
        verify(retentionStore, never()).claimExpired(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt());
        verify(storage, never()).delete(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void leavesClaimRecoverableWhenObjectDeletionFails() {
        UUID organizationId = UUID.randomUUID();
        var export = new ClinicalExportRetentionStore.ExpiredClinicalExport(
                UUID.randomUUID(), organizationId + "/" + UUID.randomUUID());
        TenantContextHolder.set(new TenantContext(organizationId, 42L, null));
        when(retentionStore.claimExpired(organizationId, 20)).thenReturn(List.of(export));
        org.mockito.Mockito.doThrow(new IllegalStateException("provider unavailable"))
                .when(storage).delete(organizationId, export.storageKey());
        var worker = new ClinicalExportRetentionWorker(retentionStore, storage, 20);

        assertThat(worker.processCurrentTenantBatch())
                .isEqualTo(new ClinicalExportRetentionWorker.RetentionBatchResult(1, 0, 1));

        verify(retentionStore, never()).markExpired(organizationId, export);
    }

    @Test
    void rejectsUnboundedBatches() {
        assertThatThrownBy(() -> new ClinicalExportRetentionWorker(retentionStore, storage, 101))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
