package com.psicogest.psicogest.dto.auth;

import com.psicogest.psicogest.model.enums.OrganizationRole;
import java.util.List;
import java.util.UUID;

public record AuthProfileResponse(
        Long userId,
        String name,
        String email,
        String role,
        Long psychoanalystId,
        UUID sessionId,
        List<OrganizationProfile> organizations
) {
    public record OrganizationProfile(
            UUID id,
            String name,
            String slug,
            String type,
            String status,
            String timezone,
            OrganizationRole role
    ) {}
}
