package com.psicogest.psicogest.service.notification;

import com.psicogest.psicogest.exception.AccessDeniedException;
import com.psicogest.psicogest.model.entity.Appointment;
import com.psicogest.psicogest.security.tenant.TenantContext;
import com.psicogest.psicogest.security.tenant.TenantContextHolder;
import com.psicogest.psicogest.security.tenant.TenantDatabaseContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Appends minimal appointment facts to the shared outbox in the caller's
 * transaction. No patient identifiers, contact details, or clinical text are
 * copied into event payloads.
 */
@Component
public class AppointmentDomainEventPublisher {

    private final JdbcTemplate jdbcTemplate;
    private final TenantDatabaseContext tenantDatabaseContext;

    public AppointmentDomainEventPublisher(
            JdbcTemplate jdbcTemplate,
            TenantDatabaseContext tenantDatabaseContext
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.tenantDatabaseContext = tenantDatabaseContext;
    }

    public void publish(Appointment appointment, AppointmentDomainEventType eventType) {
        if (appointment == null || appointment.getId() == null || appointment.getPatient() == null) {
            throw new IllegalArgumentException("Consulta persistida obrigatória para publicar evento");
        }

        TenantContext tenant = TenantContextHolder.get();
        UUID organizationId = tenant == null ? null : tenant.organizationId();
        if (organizationId == null) {
            throw new AccessDeniedException("Contexto de organização obrigatório para publicar evento");
        }

        UUID patientOrganizationId = appointment.getPatient().getOrganizationId();
        if (patientOrganizationId != null && !organizationId.equals(patientOrganizationId)) {
            throw new AccessDeniedException("Consulta fora da organização atual");
        }

        tenantDatabaseContext.applyOrganization(organizationId);
        String aggregateId = appointment.getId().toString();
        String deduplicationKey = organizationId + ":appointment:" + aggregateId + ":" + eventType.name();

        jdbcTemplate.update("""
                INSERT INTO domain_event_outbox (
                    id, aggregate_type, aggregate_id, event_type, deduplication_key,
                    payload, status, occurred_at, created_at, organization_id
                ) VALUES (?, 'APPOINTMENT', ?, ?, ?, '{}'::jsonb, 'PENDING', now(), now(), ?)
                ON CONFLICT (deduplication_key) DO NOTHING
                """,
                UUID.randomUUID(),
                aggregateId,
                eventType.name(),
                deduplicationKey,
                organizationId);
    }
}
