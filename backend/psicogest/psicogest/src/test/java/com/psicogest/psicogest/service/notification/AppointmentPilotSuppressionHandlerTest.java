package com.psicogest.psicogest.service.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import com.psicogest.psicogest.security.audit.AuditCommand;
import com.psicogest.psicogest.security.audit.AuditService;
import com.psicogest.psicogest.security.tenant.TenantContext;
import com.psicogest.psicogest.security.tenant.TenantContextHolder;

@ExtendWith(MockitoExtension.class)
class AppointmentPilotSuppressionHandlerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private AuditService auditService;

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void persistsOnlyMinimalSuppressionDataAndAuditsOnceForAnOutboxEvent() {
        UUID organizationId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        TenantContextHolder.set(new TenantContext(organizationId, 12L, null));
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
        var handler = new AppointmentPilotSuppressionHandler(jdbcTemplate, auditService);
        var event = new StoredNotificationDomainEvent(
                eventId,
                "481",
                "APPOINTMENT_CREATED",
                Instant.now(),
                Map.of("patientName", "Synthetic private name", "phone", "+5500000000000"));

        handler.handle(event);

        ArgumentCaptor<Object[]> parameters = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).update(anyString(), parameters.capture());
        assertThat(List.of(parameters.getValue()))
                .doesNotContain("Synthetic private name", "+5500000000000")
                .contains(organizationId, "APPOINTMENT_CREATED", "481",
                        "domain-event:" + eventId, "PILOT_CHANNELS_NOT_CONFIGURED");
        ArgumentCaptor<AuditCommand> audits = ArgumentCaptor.forClass(AuditCommand.class);
        verify(auditService, org.mockito.Mockito.times(2)).recordCriticalWrite(audits.capture());
        assertThat(audits.getAllValues()).allSatisfy(command -> {
            assertThat(command.metadata()).containsEntry("reasonCode", "PILOT_CHANNELS_NOT_CONFIGURED");
            assertThat(command.metadata().values()).doesNotContain("Synthetic private name", "+5500000000000");
            assertThat(command.patientId()).isNull();
        });
    }

    @Test
    void duplicateOutboxEventDoesNotCreateDuplicateAuditEntries() {
        TenantContextHolder.set(new TenantContext(UUID.randomUUID(), 12L, null));
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(0);
        var handler = new AppointmentPilotSuppressionHandler(jdbcTemplate, auditService);
        handler.handle(new StoredNotificationDomainEvent(
                UUID.randomUUID(), "481", "APPOINTMENT_CREATED", Instant.now(), Map.of()));

        verify(auditService, never()).recordCriticalWrite(any(AuditCommand.class));
    }
}
