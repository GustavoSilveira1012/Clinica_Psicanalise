package com.psicogest.psicogest.security.pilot;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class ClinicalPilotScopeGuardTest {

    @Test
    void blocksExcludedModulesAndPaymentMutationsInClinicalPilot() {
        ClinicalPilotScopeGuard guard = new ClinicalPilotScopeGuard(true, true, true);

        assertThat(guard.isUnavailable("GET", "/service-invoices")).isTrue();
        assertThat(guard.isUnavailable("GET", "/api/v1/notifications/deliveries")).isTrue();
        assertThat(guard.isUnavailable("GET", "/api/v1/notifications/preferences")).isTrue();
        assertThat(guard.isUnavailable("GET", "/organizations/8f386cc1-c0c1-4ceb-9a0a-30361caf5077/billing")).isTrue();
        assertThat(guard.isUnavailable("GET", "/organizations/8f386cc1-c0c1-4ceb-9a0a-30361caf5077/entitlements")).isTrue();
        assertThat(guard.isUnavailable("GET", "/organizations/8f386cc1-c0c1-4ceb-9a0a-30361caf5077/members")).isFalse();
        assertThat(guard.isUnavailable("POST", "/api/v1/subscription-plans")).isTrue();
        assertThat(guard.isUnavailable("POST", "/api/v1/payments")).isTrue();
        assertThat(guard.isUnavailable("POST", "/webhooks/payments/stripe")).isTrue();
        assertThat(guard.isUnavailable("POST", "/patients/12/subscriptions")).isTrue();
    }

    @Test
    void returnsGenericNotFoundForDisabledModuleEvenBehindAContextPath() throws Exception {
        ClinicalPilotScopeGuard guard = new ClinicalPilotScopeGuard(true, true, true);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/api/v1/payments");
        request.setContextPath("/api");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(guard.preHandle(request, response, new Object())).isFalse();
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getContentAsString()).contains("FEATURE_UNAVAILABLE");
    }

    @Test
    void keepsReadOnlyPaymentHistoryAndClinicalRoutesAvailable() {
        ClinicalPilotScopeGuard guard = new ClinicalPilotScopeGuard(true, true, true);

        assertThat(guard.isUnavailable("GET", "/api/v1/payments")).isFalse();
        assertThat(guard.isUnavailable("GET", "/patients")).isFalse();
        assertThat(guard.isUnavailable("POST", "/patients/12/medical-records")).isFalse();
    }

    @Test
    void doesNotRestrictModulesOutsideTheClinicalPilot() throws Exception {
        ClinicalPilotScopeGuard guard = new ClinicalPilotScopeGuard(false, true, true);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/service-invoices");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(guard.preHandle(request, response, new Object())).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void blocksSensitiveReadsAndWritesUntilClinicalDataIsExplicitlyEnabled() {
        ClinicalPilotScopeGuard guard = new ClinicalPilotScopeGuard(true, false, true);

        assertThat(guard.isUnavailable("GET", "/patients")).isTrue();
        assertThat(guard.isUnavailable("GET", "/psychoanalysts/7/appointments")).isTrue();
        assertThat(guard.isUnavailable("GET", "/medical-records/3")).isTrue();
        assertThat(guard.isUnavailable("GET", "/api/v1/receivables")).isTrue();
        assertThat(guard.isUnavailable("GET", "/api/v1/payments")).isTrue();
        assertThat(guard.isUnavailable("GET", "/api/v1/compliance/privacy-requests")).isTrue();
        assertThat(guard.isUnavailable("GET", "/api/v1/notifications/preferences")).isTrue();
        assertThat(guard.isUnavailable("POST", "/patients")).isTrue();
        assertThat(guard.isUnavailable("GET", "/health/ready")).isFalse();
    }

    @Test
    void returnsLockedWithGenericCodeWhenClinicalDataAccessIsDisabled() throws Exception {
        ClinicalPilotScopeGuard guard = new ClinicalPilotScopeGuard(true, false, true);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/patients");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(guard.preHandle(request, response, new Object())).isFalse();
        assertThat(response.getStatus()).isEqualTo(423);
        assertThat(response.getContentAsString()).contains("CLINICAL_DATA_DISABLED");
        assertThat(response.getContentAsString()).doesNotContain("patient", "tenant", "organization");
    }

    @Test
    void requiresIndependentReleaseApprovalEvenWhenClinicalDataFlagIsEnabled() {
        ClinicalPilotScopeGuard guard = new ClinicalPilotScopeGuard(true, true, false);

        assertThat(guard.isUnavailable("GET", "/patients")).isTrue();
        assertThat(guard.isUnavailable("GET", "/medical-records/3")).isTrue();
    }
}
