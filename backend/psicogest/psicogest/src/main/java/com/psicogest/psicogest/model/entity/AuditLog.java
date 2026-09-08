package com.psicogest.psicogest.model.entity;

import com.psicogest.psicogest.security.audit.AuditAction;
import com.psicogest.psicogest.security.audit.AuditOutcome;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_logs")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    private UUID id;

    @Column(
            nullable = false,
            unique = true
    )
    private Long sequence;

    @Column(name = "previous_mac")
    private String previousMac;

    @Column(
            name = "entry_mac",
            nullable = false
    )
    private String entryMac;

    @Column(
            name = "key_id",
            nullable = false
    )
    private String keyId;

    @Column(
            name = "metadata_hash",
            nullable = false
    )
    private String metadataHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actorUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private UserSession session;

    @Enumerated(EnumType.STRING)
    private AuditAction action;

    @Column(name = "resource_type")
    private String resourceType;

    @Column(name = "resource_id")
    private String resourceId;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "clinic_context_id")
    private Long clinicContextId;

    @Enumerated(EnumType.STRING)
    private AuditOutcome outcome;

    @Column(name = "occurred_at")
    private Instant occurredAt;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "source_ip")
    private String sourceIp;

    @Column(name = "user_agent_hash")
    private String userAgentHash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            columnDefinition = "jsonb"
    )
    private Map<String, Object> metadata;

    public void setEntryMac(
            String entryMac
    ) {
        this.entryMac = entryMac;
    }
}