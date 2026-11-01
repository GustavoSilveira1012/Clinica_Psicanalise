package com.psicogest.psicogest.security.pilot;

import com.psicogest.psicogest.integration.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureMockMvc
class ClinicalPilotScopeIntegrationTest extends PostgresIntegrationTest {

    @DynamicPropertySource
    static void configurePilotFlags(DynamicPropertyRegistry registry) {
        registry.add("app.pilot.clinical-only", () -> true);
        registry.add("app.pilot.clinical-data-enabled", () -> false);
        registry.add("app.pilot.clinical-data-release-approved", () -> false);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClinicalPilotScopeGuard scopeGuard;

    @Test
    void blocksExcludedCommercialAndFiscalApisAtTheHttpBoundary() throws Exception {
        assertThat(scopeGuard.isUnavailable("GET", "/service-invoices")).isTrue();
        assertThat(scopeGuard.isUnavailable("GET", "/api/v1/notifications/deliveries")).isTrue();
        var authenticated = SecurityMockMvcRequestPostProcessors.user("synthetic-pilot-user")
                .roles("CLINIC_ADMIN");

        mockMvc.perform(get("/service-invoices").with(authenticated))
                .andExpect(status().isNotFound())
                .andExpect(content().json("""
                        {"code":"FEATURE_UNAVAILABLE","message":"Módulo indisponível neste ambiente."}
                        """));

        mockMvc.perform(get("/api/v1/notifications/deliveries").with(authenticated))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(404, 423))
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .containsAnyOf("FEATURE_UNAVAILABLE", "CLINICAL_DATA_DISABLED"));
    }

    @Test
    void blocksClinicalDataAtTheHttpBoundaryUntilBothReleaseFlagsAreEnabled() throws Exception {
        assertThat(scopeGuard.isUnavailable("GET", "/patients")).isTrue();
        mockMvc.perform(get("/patients")
                        .with(SecurityMockMvcRequestPostProcessors.user("synthetic-pilot-user")
                                .roles("CLINIC_ADMIN")))
                .andExpect(status().isLocked())
                .andExpect(content().json("""
                        {"code":"CLINICAL_DATA_DISABLED","message":"Acesso a dados clínicos está desabilitado neste ambiente."}
                        """));

        mockMvc.perform(get("/users")
                        .with(SecurityMockMvcRequestPostProcessors.user("synthetic-pilot-user")
                                .roles("CLINIC_ADMIN")))
                .andExpect(status().isLocked());

        mockMvc.perform(get("/psychoanalysts/7/therapeutic-relationships")
                        .with(SecurityMockMvcRequestPostProcessors.user("synthetic-pilot-user")
                                .roles("CLINIC_ADMIN")))
                .andExpect(status().isLocked());

        mockMvc.perform(get("/api/v1/medical-record-addendums/00000000-0000-0000-0000-000000000003")
                        .with(SecurityMockMvcRequestPostProcessors.user("synthetic-pilot-user")
                                .roles("CLINIC_ADMIN")))
                .andExpect(status().isLocked());
    }
}
