package com.psicogest.psicogest.security.tenant;

import com.psicogest.psicogest.integration.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/** PostgreSQL-level tenant isolation checks using synthetic fixtures and a non-owner, non-BYPASSRLS role. */
class TenantRowLevelSecurityIntegrationTest extends PostgresIntegrationTest {

    private static final String RLS_ROLE = "psicogest_rls_test";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeAll
    static void createRestrictedRole() throws Exception {
        var result = POSTGRES.execInContainer("psql", "-U", POSTGRES.getUsername(), "-d", POSTGRES.getDatabaseName(),
                "-c", "DO $$ BEGIN IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '" + RLS_ROLE +
                        "') THEN CREATE ROLE " + RLS_ROLE + " NOLOGIN NOSUPERUSER NOBYPASSRLS; END IF; END $$; " +
                        "GRANT USAGE ON SCHEMA app TO " + RLS_ROLE + "; " +
                        "GRANT EXECUTE ON FUNCTION app.current_organization_id() TO " + RLS_ROLE + "; " +
                        "GRANT SELECT, UPDATE ON TABLE patients TO " + RLS_ROLE + ";");
        assertThat(result.getExitCode()).isZero();
    }

    @Test
    void databasePolicyHidesOtherOrganizationsPatientsFromRestrictedRuntimeRole() {
        TenantFixture fixture = createTenantFixture();

        List<Long> visiblePatientIds = inOrganizationRole(fixture.organizationA(),
                () -> jdbc.queryForList("SELECT id FROM patients ORDER BY id", Long.class));

        assertThat(visiblePatientIds).containsExactly(fixture.patientA());
        assertThat(visiblePatientIds).doesNotContain(fixture.patientB());
    }

    @Test
    void databasePolicyPreventsUpdatingAnotherOrganizationsPatient() {
        TenantFixture fixture = createTenantFixture();

        int updated = inOrganizationRole(fixture.organizationA(),
                () -> jdbc.update("UPDATE patients SET phone = '555-0100' WHERE id = ?", fixture.patientB()));

        assertThat(updated).isZero();
        assertThat(jdbc.queryForObject("SELECT phone FROM patients WHERE id = ?", String.class, fixture.patientB()))
                .isNull();
    }

    private TenantFixture createTenantFixture() {
        UUID organizationA = createOrganization("rls-a");
        UUID organizationB = createOrganization("rls-b");
        long userA = createUser("patient-a");
        long userB = createUser("patient-b");
        long patientA = createPatient(userA, organizationA);
        long patientB = createPatient(userB, organizationB);
        return new TenantFixture(organizationA, organizationB, patientA, patientB);
    }

    private UUID createOrganization(String label) {
        long ownerId = createUser("owner-" + label);
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO organizations(id, name, slug, type, owner_user_id, created_at, updated_at)
                VALUES (?, ?, ?, 'CLINIC', ?, now(), now())
                """, id, "Synthetic " + label, "synthetic-" + label + "-" + UUID.randomUUID(), ownerId);
        return id;
    }

    private long createUser(String label) {
        return jdbc.queryForObject("""
                INSERT INTO users(name, email, password_hash, role)
                VALUES (?, ?, 'not-a-usable-hash', 'CLINIC_ADMIN')
                RETURNING id
                """, Long.class, "Synthetic " + label,
                label + "-" + UUID.randomUUID() + "@example.invalid");
    }

    private long createPatient(long userId, UUID organizationId) {
        return jdbc.queryForObject("""
                INSERT INTO patients(user_id, organization_id)
                VALUES (?, ?)
                RETURNING id
                """, Long.class, userId, organizationId);
    }

    private <T> T inOrganizationRole(UUID organizationId, Supplier<T> action) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            jdbc.execute("SET LOCAL ROLE " + RLS_ROLE);
            jdbc.queryForObject("SELECT set_config('app.organization_id', ?, true)", String.class,
                    organizationId.toString());
            return action.get();
        });
    }

    private record TenantFixture(UUID organizationA, UUID organizationB, long patientA, long patientB) { }
}
