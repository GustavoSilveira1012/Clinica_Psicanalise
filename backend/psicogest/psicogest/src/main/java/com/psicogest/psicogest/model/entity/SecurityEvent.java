package com.psicogest.psicogest.model.entity;

import com.psicogest.psicogest.model.enums.SecurityEventOutcome;
import com.psicogest.psicogest.model.enums.SecurityEventSeverity;
import com.psicogest.psicogest.model.enums.SecurityEventType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "security_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityEvent {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @Column(name = "session_id")
    private UUID sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private SecurityEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private SecurityEventSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false)
    private SecurityEventOutcome outcome;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "source_ip", length = 45)
    private String sourceIp;

    @Column(name = "user_agent_hash", length = 64)
    private String userAgentHash;

    @Column(name = "request_method", length = 10)
    private String requestMethod;

    @Column(name = "request_path", length = 500)
    private String requestPath;

    @Column(name = "resource_type", length = 100)
    private String resourceType;

    @Column(name = "resource_id", length = 100)
    private String resourceId;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;
}
