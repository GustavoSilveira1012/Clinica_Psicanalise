package com.psicogest.psicogest.security.tenant;

import java.util.UUID;

public record TenantContext(
        UUID organizationId,
        Long userId,
        UUID sessionId
) {
}
