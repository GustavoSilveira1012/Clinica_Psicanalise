package com.psicogest.psicogest.security.tenant;

import com.psicogest.psicogest.exception.AccessDeniedException;
import com.psicogest.psicogest.model.entity.saas.Organization;
import com.psicogest.psicogest.model.entity.saas.OrganizationMembership;
import com.psicogest.psicogest.model.enums.OrganizationRole;
import com.psicogest.psicogest.model.enums.OrganizationType;
import com.psicogest.psicogest.repository.OrganizationMembershipRepository;
import com.psicogest.psicogest.security.authorization.AuthenticatedUserContext;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class DefaultTenantContextResolverTest {

    @Test
    void rejectsRequestedOrganizationWithoutAnActiveMembership() {
        AuthenticatedUserContext userContext = new AuthenticatedUserContext();
        OrganizationMembershipRepository memberships = mock(OrganizationMembershipRepository.class);
        TenantDatabaseContext databaseContext = mock(TenantDatabaseContext.class);
        DefaultTenantContextResolver resolver = new DefaultTenantContextResolver(userContext, memberships, databaseContext);
        var authentication = UsernamePasswordAuthenticationToken.authenticated("71001", "n/a", java.util.List.of());
        UUID otherOrganization = UUID.randomUUID();

        when(memberships.findByOrganizationIdAndUserIdAndStatus(
                eq(otherOrganization), eq(71001L), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolve(authentication, otherOrganization))
                .isInstanceOf(AccessDeniedException.class);
        verify(databaseContext).applyUser(71001L);
        verify(databaseContext, never()).applyOrganization(any());
    }

    @Test
    void bindsOnlyTheOrganizationFromTheUsersActiveMembership() {
        AuthenticatedUserContext userContext = new AuthenticatedUserContext();
        OrganizationMembershipRepository memberships = mock(OrganizationMembershipRepository.class);
        TenantDatabaseContext databaseContext = mock(TenantDatabaseContext.class);
        DefaultTenantContextResolver resolver = new DefaultTenantContextResolver(userContext, memberships, databaseContext);
        var authentication = UsernamePasswordAuthenticationToken.authenticated("71002", "n/a", java.util.List.of());
        UUID organizationId = UUID.randomUUID();
        Organization organization = Organization.builder()
                .id(organizationId)
                .name("Synthetic clinic")
                .slug("synthetic-clinic")
                .type(OrganizationType.CLINIC)
                .build();
        OrganizationMembership membership = OrganizationMembership.builder()
                .organization(organization)
                .role(OrganizationRole.OWNER)
                .build();
        when(memberships.findByOrganizationIdAndUserIdAndStatus(eq(organizationId), eq(71002L), any()))
                .thenReturn(Optional.of(membership));

        TenantContext context = resolver.resolve(authentication, organizationId);

        assertThat(context.organizationId()).isEqualTo(organizationId);
        assertThat(context.userId()).isEqualTo(71002L);
        verify(databaseContext).applyUser(71002L);
        verify(databaseContext).applyOrganization(organizationId);
    }
}
