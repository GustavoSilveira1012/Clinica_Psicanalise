package com.psicogest.psicogest.service.notification;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.psicogest.psicogest.dto.NotificationDeliveryResponse;
import com.psicogest.psicogest.dto.NotificationPreferenceResponse;
import com.psicogest.psicogest.exception.AccessDeniedException;
import com.psicogest.psicogest.exception.NotificationPreferenceException;
import com.psicogest.psicogest.model.enums.NotificationType;
import com.psicogest.psicogest.model.enums.OrganizationMembershipStatus;
import com.psicogest.psicogest.model.enums.OrganizationRole;
import com.psicogest.psicogest.repository.OrganizationMembershipRepository;
import com.psicogest.psicogest.security.SecurityActor;
import com.psicogest.psicogest.security.audit.AuditAction;
import com.psicogest.psicogest.security.audit.AuditCommand;
import com.psicogest.psicogest.security.audit.AuditOutcome;
import com.psicogest.psicogest.security.audit.AuditService;
import com.psicogest.psicogest.security.tenant.TenantContext;
import com.psicogest.psicogest.security.tenant.TenantContextHolder;

@Service
public class NotificationQueryService {

    private final JdbcTemplate jdbcTemplate;
    private final AuditService auditService;
    private final OrganizationMembershipRepository membershipRepository;

    public NotificationQueryService(
            JdbcTemplate jdbcTemplate,
            AuditService auditService,
            OrganizationMembershipRepository membershipRepository
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditService = auditService;
        this.membershipRepository = membershipRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationDeliveryResponse> listDeliveries() {
        return jdbcTemplate.query("""
                SELECT d.id, d.created_at, d.channel, n.notification_type, d.status,
                       d.provider_message_id,
                       COALESCE(patient_user.name, direct_user.name) AS recipient_name
                  FROM notification_deliveries d
                  JOIN notifications n ON n.id = d.notification_id
                  JOIN notification_recipients r ON r.id = d.recipient_id
                  LEFT JOIN patients p ON p.id = r.patient_id
                  LEFT JOIN users patient_user ON patient_user.id = p.user_id
                  LEFT JOIN users direct_user ON direct_user.id = r.user_id
                 ORDER BY d.created_at DESC
                 LIMIT 100
                """, (rs, rowNum) -> new NotificationDeliveryResponse(
                rs.getObject("id", UUID.class),
                instant(rs, "created_at"),
                maskName(rs.getString("recipient_name")),
                rs.getString("channel"),
                rs.getString("notification_type"),
                rs.getString("status"),
                rs.getString("provider_message_id")));
    }

    @Transactional(readOnly = true)
    public List<NotificationPreferenceResponse> listPreferences() {
        return jdbcTemplate.query("""
                SELECT id, patient_id, notification_type, channel, enabled
                  FROM notification_preferences
                 ORDER BY notification_type, channel
                 LIMIT 100
                """, (rs, rowNum) -> toPreference(
                rs.getObject("id", UUID.class),
                rs.getString("notification_type"),
                rs.getString("channel"),
                rs.getBoolean("enabled")));
    }

    @Transactional
    public NotificationPreferenceResponse updatePreference(
            UUID id,
            boolean enabled,
            SecurityActor actor
    ) {
        requireConfigurationManager(actor);
        PreferenceRow existing = jdbcTemplate.query(
                """
                SELECT id, patient_id, notification_type, channel, enabled
                  FROM notification_preferences
                 WHERE id = ?
                """,
                ps -> ps.setObject(1, id),
                (rs, rowNum) -> new PreferenceRow(
                        rs.getObject("id", UUID.class),
                        rs.getObject("patient_id", Long.class),
                        rs.getString("notification_type"),
                        rs.getString("channel"),
                        rs.getBoolean("enabled")))
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotificationPreferenceException("Preferência não encontrada"));

        if (isRequired(existing.notificationType())) {
            throw new NotificationPreferenceException(
                    "Comunicações transacionais obrigatórias não podem ser desativadas");
        }

        jdbcTemplate.update(
                "UPDATE notification_preferences SET enabled = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                enabled,
                id);

        auditService.recordCriticalWrite(new AuditCommand(
                actor == null ? null : actor.userId(),
                actor == null ? null : actor.sessionId(),
                AuditAction.NOTIFICATION_PREFERENCE_UPDATED,
                "NOTIFICATION_PREFERENCE",
                id.toString(),
                existing.patientId(),
                null,
                AuditOutcome.SUCCESS,
                actor == null ? "notification-preference" : actor.correlationId(),
                actor == null ? null : actor.sourceIp(),
                actor == null ? null : actor.userAgentHash(),
                Map.of("notificationType", existing.notificationType(), "channel", existing.channel(), "enabled", enabled)));

        return toPreference(existing.id(), existing.notificationType(), existing.channel(), enabled);
    }

    private NotificationPreferenceResponse toPreference(
            UUID id,
            String notificationType,
            String channel,
            boolean enabled
    ) {
        NotificationCopy copy = copyFor(notificationType);
        return new NotificationPreferenceResponse(
                id,
                notificationType,
                channel,
                copy.label(),
                copy.description(),
                enabled,
                isRequired(notificationType));
    }

    private static NotificationCopy copyFor(String rawType) {
        try {
            return switch (NotificationType.valueOf(rawType)) {
                case APPOINTMENT_CREATED, APPOINTMENT_CONFIRMED, APPOINTMENT_REMINDER,
                        APPOINTMENT_RESCHEDULED, APPOINTMENT_CANCELLED -> new NotificationCopy(
                        "Confirmação e lembrete de consulta",
                        "Mensagens operacionais para reduzir faltas.");
                case PAYMENT_CONFIRMED, PAYMENT_REMINDER, RECEIVABLE_OVERDUE -> new NotificationCopy(
                        "Avisos financeiros",
                        "Vencimentos, pagamentos e documentos financeiros.");
                case PACKAGE_ACTIVATED, PACKAGE_EXPIRING, PACKAGE_EXPIRED,
                        SUBSCRIPTION_PAYMENT_FAILED, SUBSCRIPTION_PAST_DUE, SUBSCRIPTION_CANCELLED -> new NotificationCopy(
                        "Uso de pacote e assinatura",
                        "Saldo de sessões e ciclos próximos do vencimento.");
                case SERVICE_INVOICE_AUTHORIZED, SERVICE_INVOICE_CANCELLED -> new NotificationCopy(
                        "Documentos fiscais",
                        "Atualizações operacionais sobre documentos fiscais.");
                case PRIVACY_INCIDENT_NOTICE -> new NotificationCopy(
                        "Incidentes de privacidade",
                        "Comunicações necessárias sobre segurança e privacidade.");
            };
        } catch (IllegalArgumentException exception) {
            return new NotificationCopy("Comunicação operacional", "Preferência gerenciada pela organização.");
        }
    }

    private static boolean isRequired(String rawType) {
        return rawType.startsWith("APPOINTMENT_")
                || rawType.startsWith("PAYMENT_")
                || rawType.startsWith("RECEIVABLE_")
                || rawType.startsWith("SERVICE_INVOICE_")
                || rawType.equals("PRIVACY_INCIDENT_NOTICE");
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        var value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private static String maskName(String name) {
        if (name == null || name.isBlank()) return "Destinatário protegido";
        String first = name.trim().split("\\s+")[0];
        return first.substring(0, 1) + "•••";
    }

    private void requireConfigurationManager(SecurityActor actor) {
        TenantContext tenant = TenantContextHolder.get();
        if (tenant == null || actor == null) {
            throw new AccessDeniedException("Contexto de organização obrigatório");
        }
        OrganizationRole role = membershipRepository
                .findByOrganizationIdAndUserIdAndStatus(
                        tenant.organizationId(), actor.userId(), OrganizationMembershipStatus.ACTIVE)
                .map(membership -> membership.getRole())
                .orElseThrow(() -> new AccessDeniedException("Usuário não pertence à organização"));
        if (role != OrganizationRole.OWNER && role != OrganizationRole.ADMIN) {
            throw new AccessDeniedException("Somente owner ou admin pode alterar preferências da organização");
        }
    }

    private record PreferenceRow(UUID id, Long patientId, String notificationType, String channel, boolean enabled) {
    }

    private record NotificationCopy(String label, String description) {
    }
}
