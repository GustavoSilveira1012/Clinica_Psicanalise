package com.psicogest.psicogest.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.psicogest.psicogest.dto.AuditEventResponse;
import com.psicogest.psicogest.dto.PrivacyRequestResponse;
import com.psicogest.psicogest.dto.SecuritySignalResponse;

@Service
public class ComplianceQueryService {

    private final JdbcTemplate jdbcTemplate;

    public ComplianceQueryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<PrivacyRequestResponse> listPrivacyRequests() {
        return jdbcTemplate.query("""
                SELECT r.id, r.request_type, r.status, r.submitted_at,
                       COALESCE(patient_user.name, request_user.name) AS subject_name
                  FROM data_subject_requests r
                  LEFT JOIN patients p ON p.id = r.patient_id
                  LEFT JOIN users patient_user ON patient_user.id = p.user_id
                  LEFT JOIN users request_user ON request_user.id = r.user_id
                 ORDER BY r.submitted_at DESC
                 LIMIT 100
                """, (rs, rowNum) -> new PrivacyRequestResponse(
                rs.getObject("id", java.util.UUID.class),
                maskName(rs.getString("subject_name")),
                privacyType(rs.getString("request_type")),
                instant(rs, "submitted_at"),
                privacyStatus(rs.getString("status"))));
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> listAuditEvents() {
        return jdbcTemplate.query("""
                SELECT a.id, a.action, a.resource_type, a.resource_id, a.outcome,
                       a.occurred_at, u.name AS actor_name
                  FROM audit_logs a
                  LEFT JOIN users u ON u.id = a.actor_user_id
                 ORDER BY a.occurred_at DESC
                 LIMIT 100
                """, (rs, rowNum) -> new AuditEventResponse(
                rs.getObject("id", java.util.UUID.class),
                rs.getString("action"),
                maskName(rs.getString("actor_name")),
                instant(rs, "occurred_at"),
                resource(rs.getString("resource_type"), rs.getString("resource_id")),
                auditSeverity(rs.getString("outcome"))));
    }

    @Transactional(readOnly = true)
    public List<SecuritySignalResponse> listSecuritySignals() {
        return jdbcTemplate.query("""
                SELECT id, event_type, severity, outcome, occurred_at,
                       COALESCE(metadata ->> 'description', request_path, 'Sinal registrado pelo mecanismo de segurança') AS description
                  FROM security_events
                 WHERE severity IN ('MEDIUM', 'HIGH', 'CRITICAL')
                    OR outcome IN ('FAILURE', 'BLOCKED', 'DETECTED')
                 ORDER BY occurred_at DESC
                 LIMIT 100
                """, (rs, rowNum) -> new SecuritySignalResponse(
                rs.getObject("id", java.util.UUID.class),
                rs.getString("event_type"),
                minimizeDescription(rs.getString("description")),
                instant(rs, "occurred_at"),
                "SUCCESS".equals(rs.getString("outcome")) ? "REVIEWED" : "OPEN",
                rs.getString("severity")));
    }

    private static String privacyType(String requestType) {
        return switch (requestType) {
            case "CORRECTION" -> "CORRECTION";
            case "DELETION", "ANONYMIZATION", "BLOCKING" -> "DELETION";
            default -> "ACCESS";
        };
    }

    private static String privacyStatus(String status) {
        return switch (status) {
            case "RECEIVED", "IDENTITY_VERIFICATION_REQUIRED", "VERIFIED" -> "OPEN";
            case "COMPLETED", "CANCELLED", "DENIED" -> "COMPLETED";
            default -> "IN_PROGRESS";
        };
    }

    private static String auditSeverity(String outcome) {
        return "SUCCESS".equals(outcome) ? "INFO" : "WARNING";
    }

    private static String resource(String type, String id) {
        if (type == null || type.isBlank()) return "Recurso protegido";
        return id == null || id.isBlank() ? type : type + " · " + id;
    }

    private static String minimizeDescription(String value) {
        if (value == null || value.isBlank()) return "Sinal registrado pelo mecanismo de segurança";
        return value.length() <= 180 ? value : value.substring(0, 180) + "…";
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        var value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private static String maskName(String name) {
        if (name == null || name.isBlank()) return "Sistema";
        String first = name.trim().split("\\s+")[0];
        return first.substring(0, 1) + "•••";
    }
}
