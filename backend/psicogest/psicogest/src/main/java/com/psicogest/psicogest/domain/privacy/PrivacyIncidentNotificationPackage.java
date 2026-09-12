package com.psicogest.psicogest.domain.privacy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Pacote técnico para revisão responsável antes de qualquer comunicação
 * regulatória ou aos titulares afetados.
 */
public record PrivacyIncidentNotificationPackage(
        UUID incidentId,
        Instant awareAt,
        List<String> affectedDataCategories,
        long estimatedAffectedSubjects,
        String riskAssessment,
        List<String> securityMeasures,
        List<String> mitigationMeasures
) {
}
