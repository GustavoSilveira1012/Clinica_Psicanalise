package com.psicogest.psicogest.model.entity;

import com.psicogest.psicogest.model.enums.SecurityAlertStatus;
import com.psicogest.psicogest.model.enums.SecurityAlertType;
import com.psicogest.psicogest.model.enums.SecurityEventSeverity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table( name = "security_alerts" )
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityAlert {

    @Id
    private UUID id;

    @Enumerated( EnumType.STRING )
    @Column( name = "alert_type", nullable = false )
    private SecurityAlertType alertType;

    @Enumerated( EnumType.STRING )
    @Column( nullable = false )
    private SecurityEventSeverity severity;

    @Enumerated( EnumType.STRING )
    @Column( nullable = false )
    private SecurityAlertStatus status;

    @Column( nullable = false )
    private String title;

    @Column( columnDefinition = "text" )
    private String description;

    @OneToOne( fetch = FetchType.LAZY )
    @JoinColumn( name = "source_event_id" )
    private SecurityEvent sourceEvent;

    @Column( name = "anomaly_score" )
    private BigDecimal anomalyScore;

    @Column( name = "risk_score" )
    private BigDecimal riskScore;

    @Column( nullable = false )
    private Instant detectedAt;

    private Instant acknowledgedAt;

    private Instant dismissedAt;

    @Column( nullable = false )
    private Instant createdAt;

    @Column( nullable = false )
    private Instant updatedAt;

    @JdbcTypeCode( SqlTypes.JSON )
    @Column( columnDefinition = "jsonb" )
    private Map<String, Object> metadata;
}
