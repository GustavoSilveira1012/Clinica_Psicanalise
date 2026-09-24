package com.psicogest.psicogest.service;

import com.psicogest.psicogest.exception.AccessDeniedException;
import com.psicogest.psicogest.model.entity.saas.OrganizationMembership;
import com.psicogest.psicogest.model.enums.*;
import com.psicogest.psicogest.repository.OrganizationMembershipRepository;
import com.psicogest.psicogest.security.tenant.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ComplianceQueryServiceTest {
    final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    final OrganizationMembershipRepository memberships = mock(OrganizationMembershipRepository.class);
    final ComplianceQueryService service = new ComplianceQueryService(jdbc, memberships);
    @AfterEach void clear() { TenantContextHolder.clear(); }
    @Test void deniesMissingContextWithoutReadingAnyData() {
        assertThatThrownBy(service::listAuditEvents).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(service::listSecuritySignals).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(service::listPrivacyRequests).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(jdbc);
    }
    @Test void billingAndOrdinaryMembersCannotReadGovernanceData() {
        UUID org = UUID.randomUUID();
        TenantContextHolder.set(new TenantContext(org, 1L, UUID.randomUUID()));
        for (var role : List.of(OrganizationRole.BILLING, OrganizationRole.MEMBER)) {
            var membership = OrganizationMembership.builder().role(role).build();
            when(memberships.findByOrganizationIdAndUserIdAndStatus(org, 1L, OrganizationMembershipStatus.ACTIVE))
                    .thenReturn(Optional.of(membership));
            assertThatThrownBy(service::listAuditEvents).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(service::listSecuritySignals).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(service::listPrivacyRequests).isInstanceOf(AccessDeniedException.class);
        }
        verifyNoInteractions(jdbc);
    }
}
