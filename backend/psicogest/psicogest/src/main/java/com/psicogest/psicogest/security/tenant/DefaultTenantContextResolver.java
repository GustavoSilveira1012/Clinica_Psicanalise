package com.psicogest.psicogest.security.tenant;

import com.psicogest.psicogest.exception.AccessDeniedException;
import com.psicogest.psicogest.exception.ResourceNotFoundException;
import com.psicogest.psicogest.model.entity.saas.OrganizationMembership;
import com.psicogest.psicogest.model.enums.OrganizationMembershipStatus;
import com.psicogest.psicogest.repository.OrganizationMembershipRepository;
import com.psicogest.psicogest.security.authorization.AuthenticatedUserContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DefaultTenantContextResolver implements TenantContextResolver {

    private final AuthenticatedUserContext authenticatedUserContext;
    private final OrganizationMembershipRepository membershipRepository;
    private final TenantDatabaseContext databaseContext;

    public DefaultTenantContextResolver(
            AuthenticatedUserContext authenticatedUserContext,
            OrganizationMembershipRepository membershipRepository,
            TenantDatabaseContext databaseContext
    ) {
        this.authenticatedUserContext = authenticatedUserContext;
        this.membershipRepository = membershipRepository;
        this.databaseContext = databaseContext;
    }

    @Override
    @Transactional(readOnly = true)
    public TenantContext resolve(Authentication authentication, UUID requestedOrganizationId) {
        Long userId = authenticatedUserContext.userId(authentication)
                .orElseThrow(() -> new AccessDeniedException("Usuário autenticado não identificado"));

        databaseContext.applyUser(userId);
        OrganizationMembership membership;

        if (requestedOrganizationId != null) {
            membership = membershipRepository.findByOrganizationIdAndUserIdAndStatus(
                            requestedOrganizationId, userId, OrganizationMembershipStatus.ACTIVE)
                    .orElseThrow(() -> new AccessDeniedException("Usuário não possui acesso a esta organização"));
        } else {
            List<OrganizationMembership> memberships = membershipRepository
                    .findAllByUserIdAndStatusOrderByCreatedAtAsc(userId, OrganizationMembershipStatus.ACTIVE);
            if (memberships.isEmpty()) {
                throw new ResourceNotFoundException("Nenhuma organização ativa encontrada");
            }
            if (memberships.size() > 1) {
                throw new AccessDeniedException("Selecione uma organização para continuar");
            }
            membership = memberships.getFirst();
        }

        UUID organizationId = membership.getOrganization().getId();
        databaseContext.applyOrganization(organizationId);
        UUID sessionId = null;
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            String rawSessionId = jwtAuthentication.getToken().getClaimAsString("sid");
            if (rawSessionId != null) {
                try {
                    sessionId = UUID.fromString(rawSessionId);
                } catch (IllegalArgumentException ignored) {
                    // A malformed optional claim must not become a tenant authority.
                }
            }
        }
        return new TenantContext(organizationId, userId, sessionId);
    }
}
