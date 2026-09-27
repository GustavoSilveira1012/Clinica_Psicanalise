package com.psicogest.psicogest.service.notification;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.psicogest.psicogest.domain.event.DomainEvent;
import com.psicogest.psicogest.model.enums.NotificationType;
import com.psicogest.psicogest.security.audit.AuditAction;
import com.psicogest.psicogest.security.audit.AuditCommand;
import com.psicogest.psicogest.security.audit.AuditOutcome;
import com.psicogest.psicogest.security.audit.AuditService;
import com.psicogest.psicogest.security.tenant.TenantContext;
import com.psicogest.psicogest.security.tenant.TenantContextHolder;

/**
 * Makes the no-provider pilot behavior explicit: appointment events are
 * recorded as suppressed, with no recipient, destination, message, or provider
 * call. The event UUID is the idempotency key because outbox delivery is
 * at-least-once.
 */
@Component
public class AppointmentPilotSuppressionHandler implements NotificationEventHandler {

    private static final String REASON = "PILOT_CHANNELS_NOT_CONFIGURED";
    private static final Set<String> SUPPORTED_EVENTS = Set.of(
            "APPOINTMENT_CREATED",
            "APPOINTMENT_CONFIRMED",
            "APPOINTMENT_RESCHEDULED",
            "APPOINTMENT_CANCELLED");

    private final JdbcTemplate jdbcTemplate;
    private final AuditService auditService;

    public AppointmentPilotSuppressionHandler(JdbcTemplate jdbcTemplate, AuditService auditService) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditService = auditService;
    }

    @Override
    public boolean supports(String eventType) {
        return SUPPORTED_EVENTS.contains(eventType);
    }

    @Override
    @Transactional
    public void handle(DomainEvent event) {
        if (!supports(event.eventType()) || !(event instanceof StoredNotificationDomainEvent stored)) {
            throw new IllegalArgumentException("Evento de consulta inválido para supressão no piloto");
        }
        TenantContext tenant = TenantContextHolder.get();
        if (tenant == null || tenant.organizationId() == null) {
            throw new IllegalStateException("Contexto de organização obrigatório para notificações");
        }
        if (stored.aggregateId() == null || !stored.aggregateId().matches("[0-9]{1,19}")) {
            throw new IllegalArgumentException("Agregado do evento de consulta inválido");
        }

        NotificationType.valueOf(stored.eventType());
        UUID notificationId = UUID.randomUUID();
        int inserted = jdbcTemplate.update("""
                INSERT INTO notifications (
                    id, organization_id, notification_type, aggregate_type,
                    aggregate_id, deduplication_key, status, suppression_reason_code,
                    scheduled_for, created_at, updated_at
                ) VALUES (?, ?, ?, 'APPOINTMENT', ?, ?, 'SUPPRESSED', ?, NULL, now(), now())
                ON CONFLICT (deduplication_key) DO NOTHING
                """,
                notificationId,
                tenant.organizationId(),
                stored.eventType(),
                stored.aggregateId(),
                "domain-event:" + stored.id(),
                REASON);

        if (inserted == 1) {
            Map<String, Object> metadata = Map.of(
                    "notificationType", stored.eventType(),
                    "sourceEventType", stored.eventType(),
                    "reasonCode", REASON);
            auditService.recordCriticalWrite(new AuditCommand(
                    tenant.userId(), tenant.sessionId(), AuditAction.NOTIFICATION_CREATED,
                    "NOTIFICATION", notificationId.toString(), null, null,
                    AuditOutcome.SUCCESS, "notification-outbox", null, null, metadata));
            auditService.recordCriticalWrite(new AuditCommand(
                    tenant.userId(), tenant.sessionId(), AuditAction.NOTIFICATION_DELIVERY_SUPPRESSED,
                    "NOTIFICATION", notificationId.toString(), null, null,
                    AuditOutcome.SUCCESS, "notification-outbox", null, null, metadata));
        }
    }
}
