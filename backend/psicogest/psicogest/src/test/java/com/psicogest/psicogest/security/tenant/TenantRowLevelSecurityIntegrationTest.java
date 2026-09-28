package com.psicogest.psicogest.security.tenant;

import com.psicogest.psicogest.integration.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/** PostgreSQL-level tenant isolation checks using synthetic fixtures and a non-owner, non-BYPASSRLS role. */
@Testcontainers(disabledWithoutDocker = true)
class TenantRowLevelSecurityIntegrationTest extends PostgresIntegrationTest {

    private static final String RLS_ROLE = "psicogest_rls_test";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void createRestrictedRole() throws Exception {
        var result = POSTGRES.execInContainer("psql", "-U", POSTGRES.getUsername(), "-d", POSTGRES.getDatabaseName(),
                "-c", "DO $$ BEGIN IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '" + RLS_ROLE +
                        "') THEN CREATE ROLE " + RLS_ROLE + " NOLOGIN NOSUPERUSER NOBYPASSRLS; END IF; END $$; " +
                        "GRANT USAGE ON SCHEMA app TO " + RLS_ROLE + "; " +
                        "GRANT EXECUTE ON FUNCTION app.current_organization_id() TO " + RLS_ROLE + "; " +
                "GRANT SELECT, UPDATE ON TABLE patients, medical_records, receivables, notifications, " +
                        "notification_preferences, domain_event_outbox, notification_outbox_receipts TO " + RLS_ROLE + "; " +
                        "GRANT DELETE ON TABLE notifications TO " + RLS_ROLE + ";");
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

    @Test
    void databasePoliciesHideClinicalFinancialNotificationAndOutboxRowsFromOtherOrganizations() {
        TenantFixture fixture = createTenantFixture();
        SensitiveTenantFixture sensitive = createSensitiveRows(fixture);

        Map<String, Integer> visibleRows = inOrganizationRole(fixture.organizationA(), () -> Map.of(
                "medical_records", countVisible("medical_records", sensitive.medicalRecordId()),
                "receivables", countVisible("receivables", sensitive.receivableId()),
                "notifications", countVisible("notifications", sensitive.notificationId()),
                "notification_preferences", countVisible("notification_preferences", sensitive.preferenceId()),
                "domain_event_outbox", countVisible("domain_event_outbox", sensitive.eventId()),
                "notification_outbox_receipts", countVisible("notification_outbox_receipts", sensitive.receiptId())
        ));

        assertThat(visibleRows.values()).containsOnly(0);
        int updatedReceivables = inOrganizationRole(fixture.organizationA(),
                () -> jdbc.update("UPDATE receivables SET description = 'cross-tenant-write' WHERE id = ?",
                        sensitive.receivableId()));
        int deletedNotifications = inOrganizationRole(fixture.organizationA(),
                () -> jdbc.update("DELETE FROM notifications WHERE id = ?", sensitive.notificationId()));
        assertThat(updatedReceivables).isZero();
        assertThat(deletedNotifications).isZero();

        assertThat(jdbc.queryForObject("SELECT description FROM receivables WHERE id = ?", String.class,
                sensitive.receivableId())).isEqualTo("synthetic receivable");
        assertThat(jdbc.queryForObject("SELECT status FROM notifications WHERE id = ?", String.class,
                sensitive.notificationId())).isEqualTo("SUPPRESSED");
    }

    private int countVisible(String table, UUID id) {
        // Table identifiers are fixed by this test, never sourced from a request.
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE id = ?", Integer.class, id);
    }

    private SensitiveTenantFixture createSensitiveRows(TenantFixture fixture) {
        UUID medicalRecordId = UUID.randomUUID();
        UUID receivableId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        UUID preferenceId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID receiptId = UUID.randomUUID();
        return new TransactionTemplate(transactionManager).execute(status -> {
            jdbc.queryForObject("SELECT set_config('app.organization_id', ?, true)", String.class,
                    fixture.organizationB().toString());
            long psychoanalystUserId = createUser("clinical-owner-b");
            long psychoanalystId = jdbc.queryForObject("""
                    INSERT INTO psychoanalysts(user_id, organization_id)
                    VALUES (?, ?)
                    RETURNING id
                    """, Long.class, psychoanalystUserId, fixture.organizationB());
            long relationshipId = jdbc.queryForObject("""
                    INSERT INTO therapeutic_relationships(
                        patient_id, psychoanalyst_id, status, started_at, organization_id
                    ) VALUES (?, ?, 'ACTIVE', now(), ?)
                    RETURNING id
                    """, Long.class, fixture.patientB(), psychoanalystId, fixture.organizationB());

            jdbc.update("""
                    INSERT INTO medical_records(
                        id, patient_id, author_psychoanalyst_id, therapeutic_relationship_id,
                        status, encrypted_content, content_iv, encrypted_dek, crypto_version,
                        crypto_algorithm, key_id, created_at, updated_at, organization_id
                    ) VALUES (?, ?, ?, ?, 'DRAFT', decode('01', 'hex'), decode('02', 'hex'),
                        decode('03', 'hex'), 1, 'AES-256-GCM', 'synthetic-test-key', now(), now(), ?)
                    """, medicalRecordId, fixture.patientB(), psychoanalystId,
                    relationshipId, fixture.organizationB());
            jdbc.update("""
                    INSERT INTO receivables(
                        id, patient_id, description, gross_amount, discount_amount, net_amount,
                        currency, due_date, status, created_at, updated_at, organization_id
                    ) VALUES (?, ?, 'synthetic receivable', 100.00, 0.00, 100.00,
                        'BRL', current_date, 'OPEN', now(), now(), ?)
                    """, receivableId, fixture.patientB(), fixture.organizationB());
            jdbc.update("""
                    INSERT INTO notifications(
                        id, notification_type, aggregate_type, aggregate_id, deduplication_key,
                        status, suppression_reason_code, created_at, updated_at, organization_id
                    ) VALUES (?, 'APPOINTMENT_CREATED', 'APPOINTMENT', 'synthetic', ?,
                        'SUPPRESSED', 'PILOT_CHANNELS_NOT_CONFIGURED', now(), now(), ?)
                    """, notificationId, "rls-test:" + notificationId, fixture.organizationB());
            jdbc.update("""
                    INSERT INTO notification_preferences(
                        id, patient_id, notification_type, channel, enabled, updated_at, organization_id
                    ) VALUES (?, ?, 'APPOINTMENT_CREATED', 'EMAIL', true, now(), ?)
                    """, preferenceId, fixture.patientB(), fixture.organizationB());
            jdbc.update("""
                    INSERT INTO domain_event_outbox(
                        id, aggregate_type, aggregate_id, event_type, deduplication_key,
                        payload, status, occurred_at, created_at, organization_id
                    ) VALUES (?, 'APPOINTMENT', 'synthetic', 'APPOINTMENT_CREATED', ?,
                        '{}'::jsonb, 'PENDING', now(), now(), ?)
                    """, eventId, "rls-event:" + eventId, fixture.organizationB());
            jdbc.update("""
                    INSERT INTO notification_outbox_receipts(id, event_id, organization_id, status)
                    VALUES (?, ?, ?, 'PENDING')
                    """, receiptId, eventId, fixture.organizationB());
            return new SensitiveTenantFixture(
                    medicalRecordId, receivableId, notificationId, preferenceId, eventId, receiptId);
        });
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

    private record SensitiveTenantFixture(
            UUID medicalRecordId,
            UUID receivableId,
            UUID notificationId,
            UUID preferenceId,
            UUID eventId,
            UUID receiptId
    ) { }
}
