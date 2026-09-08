package com.psicogest.psicogest.security.audit;

import java.util.Map;
import java.util.UUID;

public record AuditCommand(

        Long actorUserId,

        UUID sessionId,

        AuditAction action,

        String resourceType,

        String resourceId,

        Long patientId,

        Long clinicContextId,

        AuditOutcome outcome,

        String correlationId,

        String sourceIp,

        String userAgentHash,

        Map<String, Object> metadata

) {
}
