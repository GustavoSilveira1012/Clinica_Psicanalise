package com.psicogest.psicogest.service.notification;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.psicogest.psicogest.security.tenant.ConfiguredOrganizationAllowlist;
import com.psicogest.psicogest.security.tenant.TenantContext;
import com.psicogest.psicogest.security.tenant.TenantContextHolder;

import lombok.extern.slf4j.Slf4j;

/**
 * Tenant-scoped polling for the notification outbox. It is opt-in, bounded,
 * and only visits operator-configured organizations; no global tenant scan.
 */
@Slf4j
@Component
@Profile("production")
@ConditionalOnProperty(name = "app.notifications.outbox.scheduler.enabled", havingValue = "true")
public class NotificationOutboxScheduler {

    private static final int MAX_BATCH_SIZE = 100;
    private static final int MIN_POLL_INTERVAL_MILLIS = 1_000;
    private static final int MAX_POLL_INTERVAL_MILLIS = 1_800_000;

    private final NotificationOutboxWorker worker;
    private final List<UUID> organizationIds;
    private final int batchSize;

    public NotificationOutboxScheduler(
            NotificationOutboxWorker worker,
            @Value("${app.notifications.outbox.scheduler.organization-ids:}") String configuredOrganizationIds,
            @Value("${app.notifications.outbox.scheduler.batch-size:25}") int batchSize,
            @Value("${app.notifications.outbox.scheduler.poll-interval-ms:30000}") int pollIntervalMillis
    ) {
        this.worker = worker;
        this.organizationIds = ConfiguredOrganizationAllowlist.parse(configuredOrganizationIds);
        if (organizationIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "Allowlist de notificações obrigatória quando o scheduler está habilitado");
        }
        if (batchSize < 1 || batchSize > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("Lote do outbox deve conter de 1 a 100 eventos");
        }
        if (pollIntervalMillis < MIN_POLL_INTERVAL_MILLIS
                || pollIntervalMillis > MAX_POLL_INTERVAL_MILLIS) {
            throw new IllegalArgumentException("Intervalo do scheduler de notificações inválido");
        }
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${app.notifications.outbox.scheduler.poll-interval-ms:30000}")
    public OutboxRunResult processConfiguredOrganizations() {
        int processedOrganizations = 0;
        int dispatchedEvents = 0;
        int failedOrganizations = 0;

        for (UUID organizationId : organizationIds) {
            try {
                // Job scope is an organization only; it never impersonates a user.
                TenantContextHolder.set(new TenantContext(organizationId, null, null));
                dispatchedEvents += worker.processCurrentTenantBatch(batchSize);
                processedOrganizations++;
            } catch (RuntimeException exception) {
                // Do not log exception/provider payloads, destinations, or event data.
                failedOrganizations++;
            } finally {
                TenantContextHolder.clear();
            }
        }

        if (failedOrganizations > 0) {
            log.warn("Notification outbox polling needs retry: failedTenants={}", failedOrganizations);
        }
        return new OutboxRunResult(processedOrganizations, dispatchedEvents, failedOrganizations);
    }

    public record OutboxRunResult(int processedOrganizations, int dispatchedEvents, int failedOrganizations) { }
}
