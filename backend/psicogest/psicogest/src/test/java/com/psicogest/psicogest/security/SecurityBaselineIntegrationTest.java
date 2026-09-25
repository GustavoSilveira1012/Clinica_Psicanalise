package com.psicogest.psicogest.security;

import com.psicogest.psicogest.integration.PostgresIntegrationTest;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class SecurityBaselineIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

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
    void authActionCompletionIsPublicButStillRequiresCsrf() throws Exception {
        mockMvc.perform(post("/auth/email/verify")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType("application/json")
                        .content("{\"token\":\"invalid\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/auth/password/reset")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType("application/json")
                        .content("{\"token\":\"invalid\",\"newPassword\":\"synthetic strong password 2026\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/auth/email/verify")
                        .contentType("application/json")
                        .content("{\"token\":\"invalid\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldProtectGovernanceEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/compliance/audit-events")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/compliance/security-signals")).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectCrossTenantRequestsBeforeClinicalOrGovernanceResourceLookup() throws Exception {
        long userId = createSyntheticTenantMember();
        UUID foreignOrganizationId = createForeignOrganization();
        var principal = SecurityMockMvcRequestPostProcessors.user(Long.toString(userId))
                .roles("CLINIC_ADMIN");

        for (String path : new String[]{
                "/patients/999999",
                "/patients/999999/medical-records",
                "/patients/999999/therapeutic-relationships",
                "/patients/999999/packages",
                "/psychoanalysts/7/appointments",
                "/psychoanalysts/7/appointment-series",
                "/medical-records/95000000-0000-0000-0000-000000000001",
                "/medical-records/95000000-0000-0000-0000-000000000001/revisions",
                "/medical-record-revisions/95000000-0000-0000-0000-000000000002",
                "/medical-record-addendums/95000000-0000-0000-0000-000000000003",
                "/api/v1/receivables",
                "/api/v1/payments",
                "/api/v1/payments/95000000-0000-0000-0000-000000000004/refunds",
                "/api/v1/notifications/deliveries",
                "/api/v1/package-plans",
                "/api/v1/compliance/privacy-requests",
                "/api/v1/compliance/audit-events",
                "/api/v1/compliance/security-signals"
        }) {
            mockMvc.perform(get(path)
                            .with(principal)
                            .header("X-Organization-Id", foreignOrganizationId))
                    .andExpect(status().isForbidden());
        }
    }

    private long createSyntheticTenantMember() {
        long userId = jdbc.queryForObject("""
                INSERT INTO users(name, email, password_hash, role)
                VALUES ('Synthetic tenant member', ?, 'not-a-usable-hash', 'CLINIC_ADMIN')
                RETURNING id
                """, Long.class, "tenant-test-" + UUID.randomUUID() + "@example.invalid");
        UUID organizationId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO organizations(id, name, slug, type, owner_user_id, created_at, updated_at)
                VALUES (?, 'Synthetic tenant', ?, 'CLINIC', ?, now(), now())
                """, organizationId, "tenant-test-" + UUID.randomUUID(), userId);
        jdbc.update("""
                INSERT INTO organization_memberships(
                    id, organization_id, user_id, role, status, created_at, updated_at
                ) VALUES (?, ?, ?, 'OWNER', 'ACTIVE', now(), now())
                """, UUID.randomUUID(), organizationId, userId);
        return userId;
    }

    private UUID createForeignOrganization() {
        long ownerUserId = jdbc.queryForObject("""
                INSERT INTO users(name, email, password_hash, role)
                VALUES ('Synthetic foreign owner', ?, 'not-a-usable-hash', 'CLINIC_ADMIN')
                RETURNING id
                """, Long.class, "foreign-owner-" + UUID.randomUUID() + "@example.invalid");
        UUID organizationId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO organizations(id, name, slug, type, owner_user_id, created_at, updated_at)
                VALUES (?, 'Synthetic foreign organization', ?, 'CLINIC', ?, now(), now())
                """, organizationId, "foreign-tenant-" + UUID.randomUUID(), ownerUserId);
        return organizationId;
    }
}
