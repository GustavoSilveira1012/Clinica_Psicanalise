package com.psicogest.psicogest.controller;

import com.psicogest.psicogest.dto.auth.AuthProfileResponse;
import com.psicogest.psicogest.model.enums.OrganizationMembershipStatus;
import com.psicogest.psicogest.repository.OrganizationMembershipRepository;
import com.psicogest.psicogest.repository.UserRepository;
import com.psicogest.psicogest.repository.PsychoanalystRepository;
import com.psicogest.psicogest.security.authorization.AuthenticatedUserContext;
import com.psicogest.psicogest.security.tenant.TenantDatabaseContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthProfileController {

    private final UserRepository users;
    private final OrganizationMembershipRepository memberships;
    private final AuthenticatedUserContext context;
    private final PsychoanalystRepository psychoanalysts;
    private final TenantDatabaseContext databaseContext;

    public AuthProfileController(UserRepository users,
                                 OrganizationMembershipRepository memberships,
                                 AuthenticatedUserContext context,
                                 PsychoanalystRepository psychoanalysts,
                                 TenantDatabaseContext databaseContext) {
        this.users = users;
        this.memberships = memberships;
        this.context = context;
        this.psychoanalysts = psychoanalysts;
        this.databaseContext = databaseContext;
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public AuthProfileResponse me(Authentication authentication) {
        Long userId = context.userId(authentication)
                .orElseThrow(() -> new AccessDeniedException("Usuário não identificado"));
        // /auth is outside TenantContextFilter. Establish transaction-local RLS
        // scope before discovering the user's memberships and clinical identity.
        databaseContext.applyUser(userId);
        var user = users.findById(userId)
                .orElseThrow(() -> new AccessDeniedException("Usuário não encontrado"));
        UUID sessionId = null;
        if (authentication instanceof JwtAuthenticationToken jwt) {
            String raw = jwt.getToken().getClaimAsString("sid");
            if (raw != null) {
                try { sessionId = UUID.fromString(raw); } catch (IllegalArgumentException ignored) { }
            }
        }
        var activeMemberships = memberships.findAllByUserIdAndStatusOrderByCreatedAtAsc(
                userId, OrganizationMembershipStatus.ACTIVE);
        Long psychoanalystId = null;
        if (!activeMemberships.isEmpty()) {
            databaseContext.applyOrganization(activeMemberships.getFirst().getOrganization().getId());
            psychoanalystId = psychoanalysts.findByUserId(userId).map(item -> item.getId()).orElse(null);
        }
        return new AuthProfileResponse(user.getId(), user.getName(), user.getEmail(), user.getRole().name(), psychoanalystId,
                sessionId,
                activeMemberships
                        .stream()
                        .map(m -> new AuthProfileResponse.OrganizationProfile(
                                m.getOrganization().getId(), m.getOrganization().getName(), m.getOrganization().getSlug(),
                                m.getOrganization().getType().name(), m.getOrganization().getStatus().name(),
                                m.getOrganization().getTimezone(), m.getRole()))
                        .toList());
    }
}
