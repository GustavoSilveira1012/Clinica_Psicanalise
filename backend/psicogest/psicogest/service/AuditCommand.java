package com.psicogest.psicogest.service;

import com.psicogest.psicogest.model.enums.AuditAction;
import com.psicogest.psicogest.model.enums.AuditOutcome;

import java.util.Map;
import java.util.UUID;

public record AuditCommand(

        Long userId,

        UUID sessionId,

        AuditAction action,

        String resourceType,

        String resourceId,

        Long patientId,

        String description,

        AuditOutcome outcome,

        String correlationId,

        String sourceIp,

        String userAgentHash,

        Map<String, Object> metadata
) {
}
