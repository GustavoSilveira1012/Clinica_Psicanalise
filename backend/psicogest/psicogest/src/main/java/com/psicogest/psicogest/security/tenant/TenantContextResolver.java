package com.psicogest.psicogest.security.tenant;

import org.springframework.security.core.Authentication;

import java.util.UUID;

public interface TenantContextResolver {
    TenantContext resolve(Authentication authentication, UUID requestedOrganizationId);
}
