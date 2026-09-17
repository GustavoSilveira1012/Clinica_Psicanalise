package com.psicogest.psicogest.dto;

import java.time.Instant;
import java.util.UUID;

public record SecuritySignalResponse(
        UUID id,
        String type,
        String description,
        Instant detectedAt,
        String status,
        String severity
) {
}
