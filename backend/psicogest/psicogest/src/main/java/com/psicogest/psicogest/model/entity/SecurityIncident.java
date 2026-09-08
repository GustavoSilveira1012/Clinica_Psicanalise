package com.psicogest.psicogest.model.entity;

import com.psicogest.psicogest.model.enums.SecurityIncidentCategory;
import com.psicogest.psicogest.model.enums.SecurityIncidentStatus;
import com.psicogest.psicogest.model.enums.SecurityEventSeverity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table( name = "security_incidents" )
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityIncident {

    @Id
    private UUID id;

    @OneToOne( fetch = FetchType.LAZY )
    @JoinColumn( name = "source_alert_id" )
    private SecurityAlert sourceAlert;

    @Enumerated( EnumType.STRING )
    private SecurityIncidentCategory category;

    @Enumerated( EnumType.STRING )
    private SecurityEventSeverity severity;

    @Enumerated( EnumType.STRING )
    private SecurityIncidentStatus status;

    private String summary;

    @Column( name = "suspected_data_breach" )
    private Boolean suspectedDataBreach;

    @Column( name = "suspected_clinical_data_exposure" )
    private Boolean suspectedClinicalDataExposure;

    @ManyToOne( fetch = FetchType.LAZY )
    @JoinColumn( name = "owner_user_id" )
    private User owner;

    private Instant detectedAt;

    private Instant containedAt;

    private Instant eradicatedAt;

    private Instant recoveryStartedAt;

    private Instant resolvedAt;

    private Instant createdAt;

    private Instant updatedAt;

    @JdbcTypeCode( SqlTypes.JSON )
    @Column( columnDefinition = "jsonb" )
    private Map<String, Object> metadata;
}
