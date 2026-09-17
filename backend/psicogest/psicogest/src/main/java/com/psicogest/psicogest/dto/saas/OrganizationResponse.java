package com.psicogest.psicogest.dto.saas;

import com.psicogest.psicogest.model.enums.OrganizationStatus;
import com.psicogest.psicogest.model.enums.OrganizationType;

import java.time.Instant;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String name,
        String slug,
        OrganizationType type,
        OrganizationStatus status,
        String timezone,
        Instant createdAt
) {
}
