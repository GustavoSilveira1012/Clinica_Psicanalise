package com.psicogest.psicogest.security.incident;

import com.psicogest.psicogest.domain.security.SecurityIncidentStateMachine;
import com.psicogest.psicogest.model.entity.SecurityAlert;
import com.psicogest.psicogest.model.entity.SecurityIncident;
import com.psicogest.psicogest.model.entity.SecurityIncidentEvent;
import com.psicogest.psicogest.model.enums.SecurityIncidentCategory;
import com.psicogest.psicogest.model.enums.SecurityIncidentEventType;
import com.psicogest.psicogest.model.enums.SecurityIncidentStatus;
import com.psicogest.psicogest.repository.SecurityIncidentEventRepository;
import com.psicogest.psicogest.repository.SecurityIncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class SecurityIncidentService {

    private final SecurityIncidentRepository incidentRepository;

    private final SecurityIncidentEventRepository eventRepository;

    private final SecurityIncidentStateMachine stateMachine;

    private final SecurityAlertPublisher alertPublisher;

    public SecurityIncidentService(
            SecurityIncidentRepository incidentRepository,
            SecurityIncidentEventRepository eventRepository,
            SecurityIncidentStateMachine stateMachine,
            SecurityAlertPublisher alertPublisher
    ) {

        this.incidentRepository = incidentRepository;
        this.eventRepository = eventRepository;
        this.stateMachine = stateMachine;
        this.alertPublisher = alertPublisher;
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public SecurityIncident openFromAlert(
            SecurityAlert alert,
            SecurityIncidentCategory category
    ) {

        if (
                incidentRepository
                        .existsBySourceAlertId(
                                alert.getId()
                        )
        ) {

            throw new IllegalStateException(
                    "O alerta já possui um incidente"
            );
        }

        Instant now = Instant.now();

        SecurityIncident incident =
                SecurityIncident.builder()

                        .id( UUID.randomUUID() )

                        .sourceAlert( alert )

                        .category( category )

                        .severity(
                                alert.getSeverity()
                        )

                        .status(
                                SecurityIncidentStatus.OPEN
                        )

                        .summary(
                                alert.getTitle()
                        )

                        .suspectedDataBreach(
                                false
                        )

                        .suspectedClinicalDataExposure(
                                false
                        )

                        .detectedAt( now )

                        .createdAt( now )

                        .updatedAt( now )

                        .metadata(
                                Map.of(
                                        "riskScore",
                                        alert.getRiskScore() != null
                                                ? alert.getRiskScore()
                                                : 0
                                )
                        )

                        .build();

        incidentRepository.save( incident );

        appendEvent(

                incident,

                null,

                SecurityIncidentEventType.CREATED,

                "Incidente criado automaticamente a partir de alerta de segurança.",

                Map.of(
                        "alertId",
                        alert.getId().toString()
                )
        );

        // Publicar notificações para sinks externos
        publishNotification( incident, alert );

        return incident;
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public SecurityIncident transitionStatus(
            UUID incidentId,
            SecurityIncidentStatus newStatus,
            Long actorUserId,
            String reason
    ) {

        SecurityIncident incident =
                incidentRepository.findById(
                        incidentId
                )
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Incidente não encontrado: "
                                        + incidentId
                                )
                        );

        stateMachine.assertTransitionValid(
                incident.getStatus(),
                newStatus
        );

        incident.setStatus( newStatus );
        incident.setUpdatedAt( Instant.now() );

        incidentRepository.save( incident );

        Map<String, Object> metadata =
                Map.of(
                        "reason",
                        reason != null ? reason : ""
                );

        SecurityIncidentEventType eventType =
                switch ( newStatus ) {
                    case INVESTIGATING ->
                            SecurityIncidentEventType
                                    .INVESTIGATION_STARTED;
                    case CONTAINED ->
                            SecurityIncidentEventType
                                    .CONTAINED;
                    case ERADICATING ->
                            SecurityIncidentEventType
                                    .ERADICATION_STARTED;
                    case RECOVERING ->
                            SecurityIncidentEventType
                                    .RECOVERY_STARTED;
                    case RESOLVED ->
                            SecurityIncidentEventType
                                    .RESOLVED;
                    case FALSE_POSITIVE ->
                            SecurityIncidentEventType
                                    .DISMISSED;
                    case OPEN ->
                            null;
                };

        if ( eventType != null ) {

            appendEvent(
                    incident,
                    actorUserId,
                    eventType,
                    "Transição para " + newStatus,
                    metadata
            );
        }

        return incident;
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void appendEvent(
            SecurityIncident incident,
            Long actorUserId,
            SecurityIncidentEventType type,
            String description,
            Map<String, Object> metadata
    ) {

        Instant now = Instant.now();

        SecurityIncidentEvent event =
                SecurityIncidentEvent.builder()

                        .id( UUID.randomUUID() )

                        .incidentId(
                                incident.getId()
                        )

                        .type( type )

                        .occurredAt( now )

                        .description(
                                description
                        )

                        .actorUserId(
                                actorUserId
                        )

                        .metadata(
                                metadata != null
                                        ? metadata
                                        : Map.of()
                        )

                        .createdAt( now )

                        .build();

        eventRepository.save( event );
    }

    private void publishNotification(
            SecurityIncident incident,
            SecurityAlert alert
    ) {

        SecurityIncidentNotification notification =
                new SecurityIncidentNotification(
                        incident.getId(),
                        alert.getId(),
                        incident.getCategory(),
                        incident.getSeverity(),
                        incident.getStatus(),
                        incident.getSummary(),
                        incident.getSuspectedDataBreach(),
                        incident.getSuspectedClinicalDataExposure(),
                        incident.getDetectedAt(),
                        alert.getDetectedAt(),
                        incident.getMetadata()
                );

        alertPublisher.publishIfCritical(
                notification
        );
    }
}
