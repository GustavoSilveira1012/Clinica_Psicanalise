package com.psicogest.psicogest.model.entity;

import com.psicogest.psicogest.security.audit.AuditAction;
import com.psicogest.psicogest.security.audit.AuditOutcome;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table( name = "audit_logs" )
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    private UUID id;

    @Column( name = "user_id", nullable = false )
    private Long userId;

    @Column( name = "session_id", nullable = false )
    private UUID sessionId;

    @Enumerated( EnumType.STRING )
    @Column( nullable = false )
    private AuditAction action;

    @Column( name = "resource_type", nullable = false )
    private String resourceType;

    @Column( name = "resource_id", nullable = false )
    private String resourceId;

    @Column( name = "patient_id" )
    private Long patientId;

    @Column( columnDefinition = "text" )
    private String description;

    @Enumerated( EnumType.STRING )
    @Column( nullable = false )
    private AuditOutcome outcome;

    @Column( name = "correlation_id", nullable = false )
    private String correlationId;

    @Column( name = "source_ip" )
    private String sourceIp;

    @Column( name = "user_agent_hash" )
    private String userAgentHash;

    @JdbcTypeCode( SqlTypes.JSON )
    @Column( columnDefinition = "jsonb" )
    private Map<String, Object> metadata;

    @Column( name = "occurred_at", nullable = false )
    private Instant occurredAt;

    @Column( name = "created_at", nullable = false, updatable = false )
    private Instant createdAt;

    // Campos para integridade da auditoria
    @Column( name = "sequence", nullable = false )
    private Long sequence;

    @Column( name = "previous_mac" )
    private String previousMac;

    @Column( name = "entry_mac" )
    private String entryMac;

    @Column( name = "metadata_hash" )
    private String metadataHash;

    @Column( name = "key_id" )
    private String keyId;

    @Column( name = "clinic_context_id" )
    private Long clinicContextId;

    @ManyToOne( fetch = FetchType.LAZY )
    @JoinColumn( name = "actor_user_id" )
    private User actorUser;

    @ManyToOne( fetch = FetchType.LAZY )
    @JoinColumn( name = "session_id", insertable = false, updatable = false )
    private UserSession session;
}
