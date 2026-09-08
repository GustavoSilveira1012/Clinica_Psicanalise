package com.psicogest.psicogest.model.entity;

import com.psicogest.psicogest.model.enums.SecurityIncidentEventType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table( name = "security_incident_events" )
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityIncidentEvent {

    @Id
    private UUID id;

    @Column( name = "incident_id", nullable = false )
    private UUID incidentId;

    @Enumerated( EnumType.STRING )
    @Column( nullable = false )
    private SecurityIncidentEventType type;

    @Column( nullable = false )
    private Instant occurredAt;

    private String description;

    @Column( name = "actor_user_id" )
    private Long actorUserId;

    @JdbcTypeCode( SqlTypes.JSON )
    @Column( columnDefinition = "jsonb" )
    private Map<String, Object> metadata;

    @Column( nullable = false )
    private Instant createdAt;
}
