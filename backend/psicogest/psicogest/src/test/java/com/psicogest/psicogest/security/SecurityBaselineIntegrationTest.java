package com.psicogest.psicogest.security;

import com.psicogest.psicogest.integration.PostgresIntegrationTest;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.postgresql.util.PSQLException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class SecurityBaselineIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRejectAnonymousPatientAccess() throws Exception {

        mockMvc.perform(
                get("/patients")
        )
        .andExpect(
                status().isUnauthorized()
        );
    }

    @Test
    void shouldRejectMalformedBearerToken() throws Exception {
        mockMvc.perform(get("/patients").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRequireCsrfForCookieAuthenticationActions() throws Exception {
        mockMvc.perform(post("/auth/refresh")).andExpect(status().isForbidden());
        mockMvc.perform(post("/auth/logout")).andExpect(status().isForbidden());
    }

    @Test
    void shouldProtectGovernanceEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/compliance/audit-events")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/compliance/security-signals")).andExpect(status().isUnauthorized());
    }
}
