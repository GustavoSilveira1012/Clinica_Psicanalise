package com.psicogest.psicogest.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.psicogest.psicogest.exception.AccessDeniedException;
import com.psicogest.psicogest.security.tenant.TenantContext;
import com.psicogest.psicogest.security.tenant.TenantContextHolder;
import com.psicogest.psicogest.security.tenant.TenantDatabaseContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * At-least-once notification consumer for one already-authorized tenant.
 * It only dispatches domain events; delivery providers are a separate boundary.
 */
@Component
public class NotificationOutboxWorker {

    private static final int MAX_BATCH_SIZE = 100;
    private static final int MAX_RETRY_DELAY_SECONDS = 21_600;
    private static final RowMapper<ClaimedEvent> CLAIMED_EVENT_MAPPER = (rs, rowNum) ->
            new ClaimedEvent(rs.getObject("receipt_id", UUID.class),
                    rs.getObject("event_id", UUID.class), rs.getInt("attempt_count"));

    private final JdbcTemplate jdbcTemplate;
    private final TenantDatabaseContext tenantDatabaseContext;
    private final ObjectMapper objectMapper;
    private final List<NotificationEventHandler> handlers;
    private final TransactionTemplate tenantTransaction;
    private final int maxAttempts;
    private final int claimLeaseSeconds;
    private final int initialRetryDelaySeconds;

    public NotificationOutboxWorker(
            JdbcTemplate jdbcTemplate,
            TenantDatabaseContext tenantDatabaseContext,
            ObjectMapper objectMapper,
            List<NotificationEventHandler> handlers,
            PlatformTransactionManager transactionManager,
            @Value("${app.notifications.outbox.max-attempts:8}") int maxAttempts,
            @Value("${app.notifications.outbox.claim-lease-seconds:300}") int claimLeaseSeconds,
            @Value("${app.notifications.outbox.initial-retry-delay-seconds:30}") int initialRetryDelaySeconds
    ) {
        if (maxAttempts < 1 || maxAttempts > 20
                || claimLeaseSeconds < 30 || initialRetryDelaySeconds < 1) {
            throw new IllegalArgumentException("Configuração do worker de notificações inválida");
        }
        this.jdbcTemplate = jdbcTemplate;
        this.tenantDatabaseContext = tenantDatabaseContext;
        this.objectMapper = objectMapper;
        this.handlers = List.copyOf(handlers);
        this.maxAttempts = maxAttempts;
        this.claimLeaseSeconds = claimLeaseSeconds;
        this.initialRetryDelaySeconds = initialRetryDelaySeconds;
        this.tenantTransaction = new TransactionTemplate(transactionManager);
        this.tenantTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Processes a bounded batch for the tenant already present in the trusted
     * request/job context. It never discovers tenants globally or bypasses RLS.
     * The caller must provide tenant context; no public endpoint invokes it.
     *
     * @return number of events successfully dispatched to their handlers
     */
    public int processCurrentTenantBatch(int requestedBatchSize) {
        if (requestedBatchSize < 1 || requestedBatchSize > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("Lote deve conter de 1 a 100 eventos");
        }
        TenantContext tenant = TenantContextHolder.get();
        if (tenant == null || tenant.organizationId() == null) {
            throw new AccessDeniedException("Contexto de organização obrigatório para processar notificações");
        }
        if (handlers.isEmpty()) {
            return 0;
        }

        UUID organizationId = tenant.organizationId();
        List<ClaimedEvent> claimed = inTenant(organizationId,
                () -> claimBatch(organizationId, requestedBatchSize));
        int processed = 0;
        for (ClaimedEvent event : claimed) {
            try {
                boolean dispatched = inTenant(organizationId,
                        () -> dispatchAndComplete(organizationId, event));
                if (dispatched) processed++;
            } catch (RuntimeException failure) {
                inTenant(organizationId, () -> {
                    recordFailure(organizationId, event, failure);
                    return null;
                });
            }
        }
        return processed;
    }

    private List<ClaimedEvent> claimBatch(UUID organizationId, int batchSize) {
        tenantDatabaseContext.applyOrganization(organizationId);

        // Receipt rows are a per-consumer cursor. The shared outbox status is
        // intentionally untouched so other domain consumers remain independent.
        jdbcTemplate.update("""
                INSERT INTO notification_outbox_receipts(event_id, organization_id, status)
                SELECT event.id, event.organization_id, 'PENDING'
                  FROM domain_event_outbox event
                 WHERE event.organization_id = app.current_organization_id()
                   AND NOT EXISTS (
                       SELECT 1 FROM notification_outbox_receipts receipt
                        WHERE receipt.organization_id = event.organization_id
                          AND receipt.event_id = event.id
                   )
                 ORDER BY event.occurred_at, event.id
                 LIMIT ?
                ON CONFLICT (organization_id, event_id) DO NOTHING
                """, batchSize);

        jdbcTemplate.update("""
                UPDATE notification_outbox_receipts
                   SET status = 'RETRY', claimed_at = NULL, next_attempt_at = now(), updated_at = now()
                 WHERE organization_id = app.current_organization_id()
                   AND status = 'PROCESSING'
                   AND claimed_at < now() - make_interval(secs => ?)
                """, claimLeaseSeconds);

        return jdbcTemplate.query("""
                WITH candidates AS (
                    SELECT id
                      FROM notification_outbox_receipts
                     WHERE organization_id = app.current_organization_id()
                       AND status IN ('PENDING', 'RETRY')
                       AND (next_attempt_at IS NULL OR next_attempt_at <= now())
                     ORDER BY created_at, id
                     LIMIT ?
                     FOR UPDATE SKIP LOCKED
                )
                UPDATE notification_outbox_receipts receipt
                   SET status = 'PROCESSING',
                       attempt_count = receipt.attempt_count + 1,
                       claimed_at = now(),
                       next_attempt_at = NULL,
                       updated_at = now()
                  FROM candidates
                 WHERE receipt.id = candidates.id
                RETURNING receipt.id AS receipt_id, receipt.event_id, receipt.attempt_count
                """, CLAIMED_EVENT_MAPPER, batchSize);
    }

    private boolean dispatchAndComplete(UUID organizationId, ClaimedEvent claimed) {
        tenantDatabaseContext.applyOrganization(organizationId);
        StoredNotificationDomainEvent event = jdbcTemplate.queryForObject("""
                SELECT id, aggregate_id, event_type, occurred_at, payload::text AS payload
                  FROM domain_event_outbox
                 WHERE id = ? AND organization_id = app.current_organization_id()
                """, (rs, rowNum) -> mapEvent(rs), claimed.eventId());
        if (event == null) {
            throw new IllegalStateException("Evento do outbox não encontrado no tenant atual");
        }

        List<NotificationEventHandler> matchingHandlers = handlers.stream()
                .filter(handler -> handler.supports(event.eventType()))
                .toList();
        if (matchingHandlers.isEmpty()) {
            jdbcTemplate.update("""
                    UPDATE notification_outbox_receipts
                       SET status = 'IGNORED', processed_at = now(), claimed_at = NULL,
                           next_attempt_at = NULL, last_error_code = 'NO_NOTIFICATION_HANDLER', updated_at = now()
                     WHERE id = ? AND event_id = ?
                       AND organization_id = app.current_organization_id()
                       AND status = 'PROCESSING'
                    """, claimed.receiptId(), claimed.eventId());
            return false;
        }

        for (NotificationEventHandler handler : matchingHandlers) {
            handler.handle(event);
        }

        jdbcTemplate.update("""
                UPDATE notification_outbox_receipts
                   SET status = 'PROCESSED', processed_at = now(), claimed_at = NULL,
                       next_attempt_at = NULL, last_error_code = NULL, updated_at = now()
                 WHERE id = ? AND event_id = ?
                   AND organization_id = app.current_organization_id()
                   AND status = 'PROCESSING'
                """, claimed.receiptId(), claimed.eventId());
        return true;
    }

    private void recordFailure(UUID organizationId, ClaimedEvent claimed, RuntimeException failure) {
        tenantDatabaseContext.applyOrganization(organizationId);
        if (claimed.attemptCount() >= maxAttempts) {
            jdbcTemplate.update("""
                    UPDATE notification_outbox_receipts
                       SET status = 'DEAD_LETTER', claimed_at = NULL, next_attempt_at = NULL,
                           last_error_code = ?, updated_at = now()
                     WHERE id = ? AND organization_id = app.current_organization_id()
                       AND status = 'PROCESSING'
                    """, safeErrorCode(failure), claimed.receiptId());
            return;
        }

        Instant retryAt = Instant.now().plus(retryDelay(claimed.attemptCount()));
        jdbcTemplate.update("""
                UPDATE notification_outbox_receipts
                   SET status = 'RETRY', claimed_at = NULL, next_attempt_at = ?,
                       last_error_code = ?, updated_at = now()
                 WHERE id = ? AND organization_id = app.current_organization_id()
                   AND status = 'PROCESSING'
                """, java.sql.Timestamp.from(retryAt), safeErrorCode(failure), claimed.receiptId());
    }

    private StoredNotificationDomainEvent mapEvent(ResultSet rs) throws SQLException {
        return StoredNotificationDomainEvent.from(
                rs.getObject("id", UUID.class),
                rs.getString("aggregate_id"),
                rs.getString("event_type"),
                rs.getTimestamp("occurred_at").toInstant(),
                rs.getString("payload"),
                objectMapper);
    }

    private Duration retryDelay(int attemptCount) {
        long multiplier = 1L << Math.min(Math.max(0, attemptCount - 1), 30);
        long millis;
        try {
            millis = Math.multiplyExact((long) initialRetryDelaySeconds * 1_000, multiplier);
        } catch (ArithmeticException overflow) {
            millis = (long) MAX_RETRY_DELAY_SECONDS * 1_000;
        }
        return Duration.ofMillis(Math.min(millis, (long) MAX_RETRY_DELAY_SECONDS * 1_000));
    }

    private String safeErrorCode(RuntimeException failure) {
        // Store only a stable class identifier; exception messages can contain
        // destinations, payload fragments, SQL values, or other sensitive data.
        String simpleName = failure.getClass().getSimpleName();
        return simpleName.length() <= 100 ? simpleName : "HANDLER_FAILURE";
    }

    private <T> T inTenant(UUID organizationId, java.util.function.Supplier<T> operation) {
        return tenantTransaction.execute(status -> {
            tenantDatabaseContext.applyOrganization(organizationId);
            return operation.get();
        });
    }

    private record ClaimedEvent(UUID receiptId, UUID eventId, int attemptCount) { }
}
