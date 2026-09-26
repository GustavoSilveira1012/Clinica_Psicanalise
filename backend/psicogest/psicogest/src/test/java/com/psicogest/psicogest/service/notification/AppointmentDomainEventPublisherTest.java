package com.psicogest.psicogest.service.notification;

import com.psicogest.psicogest.exception.AccessDeniedException;
import com.psicogest.psicogest.model.entity.Appointment;
import com.psicogest.psicogest.model.entity.Patient;
import com.psicogest.psicogest.security.tenant.TenantContext;
import com.psicogest.psicogest.security.tenant.TenantContextHolder;
import com.psicogest.psicogest.security.tenant.TenantDatabaseContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AppointmentDomainEventPublisherTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final TenantDatabaseContext tenantDatabaseContext = mock(TenantDatabaseContext.class);
    private final AppointmentDomainEventPublisher publisher =
            new AppointmentDomainEventPublisher(jdbcTemplate, tenantDatabaseContext);

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void appendsTenantScopedEventWithEmptyPayloadAndIdempotentKey() {
        UUID organizationId = UUID.randomUUID();
        TenantContextHolder.set(new TenantContext(organizationId, 12L, UUID.randomUUID()));
        Appointment appointment = appointment(41L, organizationId);

        publisher.publish(appointment, AppointmentDomainEventType.APPOINTMENT_CREATED);

        verify(tenantDatabaseContext).applyOrganization(organizationId);
        var sql = org.mockito.ArgumentCaptor.forClass(String.class);
        var args = org.mockito.ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).update(sql.capture(), args.capture());
        assertThat(sql.getValue()).contains("'{}'::jsonb", "ON CONFLICT (deduplication_key) DO NOTHING");
        assertThat(args.getValue()).contains(organizationId, "41", "APPOINTMENT_CREATED",
                organizationId + ":appointment:41:APPOINTMENT_CREATED");
        assertThat(args.getValue()).noneMatch(value -> value instanceof String text
                && (text.contains("patient") || text.contains("@") || text.contains("phone")));
    }

    @Test
    void rejectsMissingOrganizationContext() {
        assertThatThrownBy(() -> publisher.publish(appointment(41L, null),
                AppointmentDomainEventType.APPOINTMENT_CREATED))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(jdbcTemplate, tenantDatabaseContext);
    }

    @Test
    void rejectsPatientFromAnotherOrganization() {
        TenantContextHolder.set(new TenantContext(UUID.randomUUID(), 12L, null));

        assertThatThrownBy(() -> publisher.publish(appointment(41L, UUID.randomUUID()),
                AppointmentDomainEventType.APPOINTMENT_CREATED))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(jdbcTemplate, tenantDatabaseContext);
    }

    private Appointment appointment(Long id, UUID organizationId) {
        return Appointment.builder()
                .id(id)
                .patient(Patient.builder().organizationId(organizationId).build())
                .build();
    }
}
