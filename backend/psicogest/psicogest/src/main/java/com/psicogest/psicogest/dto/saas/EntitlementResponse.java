package com.psicogest.psicogest.dto.saas;

public record EntitlementResponse(
        String featureCode,
        boolean enabled,
        Long limit
) {
}
