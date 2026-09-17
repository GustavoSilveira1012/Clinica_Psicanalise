package com.psicogest.psicogest.dto;

import java.time.Instant;
import java.util.UUID;

public record PrivacyRequestResponse(
        UUID id,
        String subjectLabel,
        String type,
        Instant receivedAt,
        String status
) {
}
