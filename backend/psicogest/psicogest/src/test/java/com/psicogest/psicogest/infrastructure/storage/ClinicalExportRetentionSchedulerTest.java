package com.psicogest.psicogest.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.psicogest.psicogest.security.tenant.TenantContextHolder;

class ClinicalExportRetentionSchedulerTest {

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void runsOnlyConfiguredTenantsAndClearsContextAfterEveryTenant() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        ClinicalExportRetentionWorker worker = mock(ClinicalExportRetentionWorker.class);
        AtomicInteger tenantInvocation = new AtomicInteger();
        when(worker.processCurrentTenantBatch()).thenAnswer(invocation -> {
            assertThat(TenantContextHolder.get()).isNotNull();
            assertThat(TenantContextHolder.get().userId()).isNull();
            assertThat(TenantContextHolder.get().organizationId())
                    .isEqualTo(tenantInvocation.getAndIncrement() == 0 ? first : second);
            return new ClinicalExportRetentionWorker.RetentionBatchResult(2, 1, 1);
        });
        var scheduler = new ClinicalExportRetentionScheduler(worker, first + "," + second);

        assertThat(scheduler.runConfiguredOrganizations()).isEqualTo(
                new ClinicalExportRetentionScheduler.RetentionRunResult(2, 4, 2, 2, 0));

        verify(worker, org.mockito.Mockito.times(2)).processCurrentTenantBatch();
        assertThat(TenantContextHolder.get()).isNull();
    }

    @Test
    void emptyAllowlistDoesNotRunAnyTenant() {
        ClinicalExportRetentionWorker worker = mock(ClinicalExportRetentionWorker.class);
        var scheduler = new ClinicalExportRetentionScheduler(worker, " , ");

        assertThat(scheduler.runConfiguredOrganizations()).isEqualTo(
                new ClinicalExportRetentionScheduler.RetentionRunResult(0, 0, 0, 0, 0));
        verify(worker, never()).processCurrentTenantBatch();
    }

    @Test
    void invalidOrOversizedTenantAllowlistIsRejected() {
        ClinicalExportRetentionWorker worker = mock(ClinicalExportRetentionWorker.class);

        assertThatThrownBy(() -> new ClinicalExportRetentionScheduler(worker, "not-a-uuid"))
                .isInstanceOf(IllegalArgumentException.class);
        String oversized = java.util.stream.IntStream.range(0, 101)
                .mapToObj(index -> UUID.nameUUIDFromBytes(("tenant-" + index).getBytes()).toString())
                .collect(java.util.stream.Collectors.joining(","));
        assertThatThrownBy(() -> new ClinicalExportRetentionScheduler(worker, oversized))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void continuesAfterTenantFailureAndDoesNotLeakTenantContext() {
        UUID failing = UUID.randomUUID();
        UUID succeeding = UUID.randomUUID();
        ClinicalExportRetentionWorker worker = mock(ClinicalExportRetentionWorker.class);
        when(worker.processCurrentTenantBatch())
                .thenThrow(new IllegalStateException("provider failure"))
                .thenReturn(new ClinicalExportRetentionWorker.RetentionBatchResult(0, 0, 0));
        var scheduler = new ClinicalExportRetentionScheduler(worker, failing + "," + succeeding);

        assertThat(scheduler.runConfiguredOrganizations()).isEqualTo(
                new ClinicalExportRetentionScheduler.RetentionRunResult(1, 0, 0, 0, 1));
        verify(worker, org.mockito.Mockito.times(2)).processCurrentTenantBatch();
        assertThat(TenantContextHolder.get()).isNull();
    }
}
