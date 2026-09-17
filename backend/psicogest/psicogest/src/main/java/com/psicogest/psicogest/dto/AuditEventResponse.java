package com.psicogest.psicogest.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        String action,
        String actor,
        Instant occurredAt,
        String resource,
        String severity
) {
}
