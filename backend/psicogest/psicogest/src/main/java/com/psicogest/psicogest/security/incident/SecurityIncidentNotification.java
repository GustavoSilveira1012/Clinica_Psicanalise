package com.psicogest.psicogest.security.incident;

import com.psicogest.psicogest.model.enums.SecurityEventSeverity;
import com.psicogest.psicogest.model.enums.SecurityIncidentCategory;
import com.psicogest.psicogest.model.enums.SecurityIncidentStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record SecurityIncidentNotification(
        UUID incidentId,
        UUID sourceAlertId,
        SecurityIncidentCategory category,
        SecurityEventSeverity severity,
        SecurityIncidentStatus status,
        String summary,
        Boolean suspectedDataBreach,
        Boolean suspectedClinicalDataExposure,
        Instant detectedAt,
        Instant occurredAt,
        Map<String, Object> metadata
) {

    public boolean isCritical() {
        return severity == SecurityEventSeverity.CRITICAL;
    }

    public boolean involvesClinicalData() {
        return Boolean.TRUE.equals(
                suspectedClinicalDataExposure
        );
    }

    public boolean involvesBreach() {
        return Boolean.TRUE.equals(
                suspectedDataBreach
        );
    }
}
