package com.psicogest.psicogest.service.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.psicogest.psicogest.security.tenant.TenantContextHolder;

class NotificationOutboxSchedulerTest {

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void processesOnlyAllowlistedTenantsWithoutUserImpersonationAndClearsContext() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        NotificationOutboxWorker worker = mock(NotificationOutboxWorker.class);
        AtomicInteger tenantInvocation = new AtomicInteger();
        when(worker.processCurrentTenantBatch(10)).thenAnswer(invocation -> {
            assertThat(TenantContextHolder.get()).isNotNull();
            assertThat(TenantContextHolder.get().userId()).isNull();
            assertThat(TenantContextHolder.get().organizationId())
                    .isEqualTo(tenantInvocation.getAndIncrement() == 0 ? first : second);
            return 3;
        });
        var scheduler = new NotificationOutboxScheduler(worker, first + "," + second, 10, 30_000);

        assertThat(scheduler.processConfiguredOrganizations()).isEqualTo(
                new NotificationOutboxScheduler.OutboxRunResult(2, 6, 0));

        verify(worker, times(2)).processCurrentTenantBatch(10);
        assertThat(TenantContextHolder.get()).isNull();
    }

    @Test
    void continuesAfterTenantFailureWithoutLeakingTenantContext() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        NotificationOutboxWorker worker = mock(NotificationOutboxWorker.class);
        when(worker.processCurrentTenantBatch(5))
                .thenThrow(new IllegalStateException("synthetic handler failure"))
                .thenReturn(2);
        var scheduler = new NotificationOutboxScheduler(worker, first + "," + second, 5, 30_000);

        assertThat(scheduler.processConfiguredOrganizations()).isEqualTo(
                new NotificationOutboxScheduler.OutboxRunResult(1, 2, 1));

        verify(worker, times(2)).processCurrentTenantBatch(5);
        assertThat(TenantContextHolder.get()).isNull();
    }

    @Test
    void refusesMissingOrMalformedAllowlistAndUnsafePollingConfiguration() {
        NotificationOutboxWorker worker = mock(NotificationOutboxWorker.class);

        assertThatThrownBy(() -> new NotificationOutboxScheduler(worker, "", 10, 30_000))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NotificationOutboxScheduler(worker, "invalid", 10, 30_000))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NotificationOutboxScheduler(worker, UUID.randomUUID().toString(), 101, 30_000))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NotificationOutboxScheduler(worker, UUID.randomUUID().toString(), 10, 999))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
