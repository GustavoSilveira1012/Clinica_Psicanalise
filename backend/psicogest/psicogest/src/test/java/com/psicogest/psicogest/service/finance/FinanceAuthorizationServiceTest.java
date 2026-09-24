package com.psicogest.psicogest.service.finance;

import com.psicogest.psicogest.exception.AuthorizationException;
import com.psicogest.psicogest.model.entity.*;
import com.psicogest.psicogest.model.enums.*;
import com.psicogest.psicogest.repository.*;
import com.psicogest.psicogest.security.SecurityActor;
import com.psicogest.psicogest.security.tenant.*;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class FinanceAuthorizationServiceTest {
    private final BankAccountRepository accounts = mock(BankAccountRepository.class);
    private final PaymentRepository payments = mock(PaymentRepository.class);
    private final ClinicUserMembershipRepository memberships = mock(ClinicUserMembershipRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final FinanceAuthorizationService service = new FinanceAuthorizationService(accounts, payments, memberships, users);
    private final UUID organization = UUID.randomUUID();
    private final SecurityActor actor = new SecurityActor(10L, UUID.randomUUID(), "test", "127.0.0.1", "test");

    @BeforeEach void context() {
        TenantContextHolder.set(new TenantContext(organization, actor.userId(), actor.sessionId()));
        User user = new User();
        user.setRole(UserRole.CLINIC_ADMIN);
        when(users.findById(actor.userId())).thenReturn(Optional.of(user));
    }
    @AfterEach void cleanup() { TenantContextHolder.clear(); }

    private void membership(UUID org, Long clinicId, boolean active, ClinicAccessRole role) {
        Clinic clinic = Clinic.builder().id(clinicId).organizationId(org).active(active).build();
        when(memberships.findByUserIdAndStatus(actor.userId(), ClinicUserMembershipStatus.ACTIVE))
            .thenReturn(List.of(ClinicUserMembership.builder().clinic(clinic).accessRole(role).build()));
    }
    @Test void permitsFinanceMembershipInSelectedOrganization() {
        membership(organization, 1L, true, ClinicAccessRole.FINANCE);
        assertThatCode(() -> service.validateClinicAccess(1L, actor)).doesNotThrowAnyException();
    }
    @Test void deniesDifferentOrganizationEvenWithSameClinicAndAdminRole() {
        membership(UUID.randomUUID(), 1L, true, ClinicAccessRole.ADMIN);
        assertThatThrownBy(() -> service.validateClinicAccess(1L, actor)).isInstanceOf(AuthorizationException.class);
    }
    @Test void deniesDifferentClinic() {
        membership(organization, 2L, true, ClinicAccessRole.FINANCE);
        assertThatThrownBy(() -> service.validateClinicAccess(1L, actor)).isInstanceOf(AuthorizationException.class);
    }
    @Test void deniesInactiveClinic() {
        membership(organization, 1L, false, ClinicAccessRole.ADMIN);
        assertThatThrownBy(() -> service.validateClinicAccess(1L, actor)).isInstanceOf(AuthorizationException.class);
    }
    @Test void deniesMissingTenantBeforeRepositoryAccess() {
        TenantContextHolder.clear();
        assertThatThrownBy(() -> service.validateClinicAccess(1L, actor)).isInstanceOf(AuthorizationException.class);
        verifyNoInteractions(memberships);
    }
    @Test void deniesImpersonatedActor() {
        TenantContextHolder.set(new TenantContext(organization, 999L, UUID.randomUUID()));
        assertThatThrownBy(() -> service.validateClinicAccess(1L, actor)).isInstanceOf(AuthorizationException.class);
        verifyNoInteractions(memberships);
    }
    @Test void deniesSystemAdministratorWithoutDelegation() {
        User user = new User(); user.setRole(UserRole.SYSTEM_ADMIN);
        when(users.findById(actor.userId())).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> service.validateClinicAccess(1L, actor)).isInstanceOf(AuthorizationException.class);
    }
    @Test void deniesGlobalRoleWithoutClinicMembership() {
        when(memberships.findByUserIdAndStatus(actor.userId(), ClinicUserMembershipStatus.ACTIVE)).thenReturn(List.of());
        assertThatThrownBy(() -> service.validateClinicAccess(1L, actor)).isInstanceOf(AuthorizationException.class);
    }
    @Test void deniesMembershipWithoutAccessRole() {
        membership(organization, 1L, true, null);
        assertThatThrownBy(() -> service.validateClinicAccess(1L, actor)).isInstanceOf(AuthorizationException.class);
    }
}
