package com.psicogest.psicogest.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.sql.*;
import static org.assertj.core.api.Assertions.*;

/** Real SQL, including upgrade and isolation under a non-superuser role. */
@Testcontainers
class MigrationLifecycleTest {
    @Container static final PostgreSQLContainer<?> DB = new PostgreSQLContainer<>("postgres:16-alpine");
    private Flyway flyway(String target) {
        return Flyway.configure().dataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword())
                .locations("classpath:bd/migration").target(target).baselineOnMigrate(false).cleanDisabled(true).load();
    }

    @Test void cleanBootstrapUpgradeAndTenantIsolation() throws Exception {
        assertThat(flyway("77").migrate().migrationsExecuted).isGreaterThan(70);
        try (Connection db = DriverManager.getConnection(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword());
             Statement sql = db.createStatement()) {
            sql.execute("INSERT INTO users(id,name,email,password_hash,role) VALUES (900001,'Synthetic','synthetic@example.invalid','not-a-usable-hash','CLINIC_ADMIN')");
            sql.execute("""
                    INSERT INTO organizations(id,name,slug,type,owner_user_id,created_at,updated_at) VALUES
                    ('10000000-0000-0000-0000-000000000001','Synthetic A','synthetic-a','CLINIC',900001,now(),now()),
                    ('20000000-0000-0000-0000-000000000002','Synthetic B','synthetic-b','CLINIC',900001,now(),now())
                    """);
            sql.execute("""
                    INSERT INTO clinics(name,organization_id) VALUES
                    ('Synthetic A','10000000-0000-0000-0000-000000000001'),
                    ('Synthetic B','20000000-0000-0000-0000-000000000002')
                    """);
            seedLegacyCredit(sql);
            assertThat(flyway("latest").migrate().migrationsExecuted).isPositive();
            flyway("latest").validate();
            assertThat(flyway("latest").migrate().migrationsExecuted).isZero();
            assertThat(count(sql, "SELECT count(*) FROM clinics")).isEqualTo(2);
            assertThat(count(sql, "SELECT quantity FROM session_credit_entries WHERE patient_id=900001")).isEqualTo(5);
            assertThatThrownBy(() -> sql.execute("UPDATE session_credit_entries SET quantity=500"))
                    .isInstanceOf(SQLException.class).hasMessageContaining("append-only");
            assertThatThrownBy(() -> sql.execute("DELETE FROM session_credit_entries"))
                    .isInstanceOf(SQLException.class).hasMessageContaining("append-only");
            assertThatThrownBy(() -> sql.execute("""
                    INSERT INTO session_credit_entries(id,patient_id,direction,entry_type,quantity,created_at,organization_id)
                    VALUES (gen_random_uuid(),900001,'CREDIT','MANUAL_ADJUSTMENT',1,now(),'20000000-0000-0000-0000-000000000002')
                    """))
                    .isInstanceOf(SQLException.class).hasMessageContaining("outside organization");

            seedTenantSensitiveRows(sql);

            sql.execute("CREATE ROLE psicogest_rls_test NOSUPERUSER NOBYPASSRLS NOLOGIN");
            sql.execute("GRANT USAGE ON SCHEMA public, app TO psicogest_rls_test");
            sql.execute("GRANT SELECT, INSERT, UPDATE ON clinics TO psicogest_rls_test");
            sql.execute("GRANT SELECT, INSERT ON session_credit_entries TO psicogest_rls_test");
            sql.execute("GRANT SELECT ON patients, patient_packages, appointments, package_plan_items, receivables, payments, notifications TO psicogest_rls_test");
            sql.execute("GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO psicogest_rls_test");
            sql.execute("SET ROLE psicogest_rls_test");
            assertThat(count(sql, "SELECT count(*) FROM clinics")).isZero();
            assertThat(count(sql, "SELECT count(*) FROM session_credit_entries")).isZero();
            db.setAutoCommit(false);
            sql.execute("SELECT set_config('app.organization_id','10000000-0000-0000-0000-000000000001',true)");
            assertThat(count(sql, "SELECT count(*) FROM clinics")).isEqualTo(1);
            assertThat(count(sql, "SELECT count(*) FROM patients WHERE id=900001")).isEqualTo(1);
            assertThat(count(sql, "SELECT count(*) FROM session_credit_entries")).isEqualTo(1);
            assertThat(count(sql, "SELECT count(*) FROM receivables")).isEqualTo(1);
            assertThat(count(sql, "SELECT count(*) FROM payments")).isEqualTo(1);
            assertThat(count(sql, "SELECT count(*) FROM notifications")).isEqualTo(1);
            sql.execute("""
                    INSERT INTO session_credit_entries(id,patient_id,direction,entry_type,quantity,created_at)
                    VALUES (gen_random_uuid(),900001,'CREDIT','MANUAL_ADJUSTMENT',1,now())
                    """);
            assertThat(sql.executeUpdate("UPDATE clinics SET name='forbidden' WHERE organization_id='20000000-0000-0000-0000-000000000002'")).isZero();
            Savepoint beforeRejectedCrossTenantInsert = db.setSavepoint();
            assertThatThrownBy(() -> sql.execute("INSERT INTO clinics(name,organization_id) VALUES ('forbidden','20000000-0000-0000-0000-000000000002')"))
                    .isInstanceOf(SQLException.class).extracting(error -> ((SQLException) error).getSQLState()).isEqualTo("42501");
            db.rollback(beforeRejectedCrossTenantInsert);
            sql.execute("SELECT set_config('app.organization_id','20000000-0000-0000-0000-000000000002',true)");
            assertThat(count(sql, "SELECT count(*) FROM patients WHERE id=900001")).as("tenant B must not read tenant A patient").isZero();
            assertThat(count(sql, "SELECT count(*) FROM receivables")).as("tenant B must not read tenant A receivables").isZero();
            assertThat(count(sql, "SELECT count(*) FROM payments")).as("tenant B must not read tenant A payments").isZero();
            assertThat(count(sql, "SELECT count(*) FROM notifications")).as("tenant B must not read tenant A notifications").isZero();
            assertThat(count(sql, "SELECT count(*) FROM clinics WHERE organization_id='10000000-0000-0000-0000-000000000001'")).isZero();
            assertCriticalTablesHaveForcedTenantPolicies(sql);
            db.rollback();
            assertThat(count(sql, "SELECT count(*) FROM clinics")).isZero();
            db.rollback();
            sql.execute("RESET ROLE");
            db.commit();
            assertThat(count(sql, "SELECT count(*) FROM clinics WHERE name='forbidden'")).isZero();
        }
    }

    private void seedTenantSensitiveRows(Statement sql) throws SQLException {
        sql.execute("""
                INSERT INTO receivables(id,patient_id,description,gross_amount,discount_amount,net_amount,due_date,status,created_at,updated_at,organization_id)
                VALUES ('91000000-0000-0000-0000-000000000001',900001,'Synthetic receivable',100,0,100,CURRENT_DATE,'OPEN',now(),now(),'10000000-0000-0000-0000-000000000001');
                INSERT INTO payments(id,patient_id,amount,payment_method,status,created_at,updated_at,organization_id)
                VALUES ('92000000-0000-0000-0000-000000000001',900001,100,'PIX','CONFIRMED',now(),now(),'10000000-0000-0000-0000-000000000001');
                INSERT INTO notifications(id,notification_type,aggregate_type,aggregate_id,deduplication_key,status,created_at,updated_at,organization_id)
                VALUES ('93000000-0000-0000-0000-000000000001','APPOINTMENT_CREATED','APPOINTMENT','synthetic-appointment',
                        'synthetic-tenant-isolation-test','PENDING',now(),now(),'10000000-0000-0000-0000-000000000001');
                """);
    }

    private void seedLegacyCredit(Statement sql) throws SQLException {
        sql.execute("""
                INSERT INTO patients(id,user_id,organization_id)
                VALUES(900001,900001,'10000000-0000-0000-0000-000000000001');
                INSERT INTO financial_entities(id,entity_type,clinic_id,created_at,updated_at,organization_id)
                SELECT '30000000-0000-0000-0000-000000000001','CLINIC',id,now(),now(),organization_id
                FROM clinics WHERE name='Synthetic A';
                INSERT INTO package_plans(id,financial_entity_id,name,status,created_at,updated_at,organization_id)
                VALUES('40000000-0000-0000-0000-000000000001','30000000-0000-0000-0000-000000000001',
                       'Synthetic plan','ACTIVE',now(),now(),'10000000-0000-0000-0000-000000000001');
                INSERT INTO package_plan_versions(id,package_plan_id,version,status,total_price,activation_policy,created_at,organization_id)
                VALUES('50000000-0000-0000-0000-000000000001','40000000-0000-0000-0000-000000000001',1,
                       'PUBLISHED',100,'ON_PAYMENT',now(),'10000000-0000-0000-0000-000000000001');
                INSERT INTO package_plan_items(id,package_plan_version_id,service_code,quantity,allocated_amount,created_at,organization_id)
                VALUES('60000000-0000-0000-0000-000000000001','50000000-0000-0000-0000-000000000001',
                       'SYNTHETIC',5,100,now(),'10000000-0000-0000-0000-000000000001');
                INSERT INTO patient_packages(id,patient_id,financial_entity_id,package_plan_version_id,status,purchased_at,created_at,updated_at,organization_id)
                VALUES('70000000-0000-0000-0000-000000000001',900001,'30000000-0000-0000-0000-000000000001',
                       '50000000-0000-0000-0000-000000000001','ACTIVE',now(),now(),now(),'10000000-0000-0000-0000-000000000001');
                INSERT INTO session_credit_entries(id,patient_package_id,package_item_id,direction,entry_type,quantity,created_at,organization_id)
                VALUES('80000000-0000-0000-0000-000000000001','70000000-0000-0000-0000-000000000001',
                       '60000000-0000-0000-0000-000000000001','CREDIT','PACKAGE_ACTIVATION',5,now(),
                       '10000000-0000-0000-0000-000000000001');
                """);
    }

    private long count(Statement sql, String query) throws SQLException {
        try (ResultSet result = sql.executeQuery(query)) { result.next(); return result.getLong(1); }
    }

    private void assertCriticalTablesHaveForcedTenantPolicies(Statement sql) throws SQLException {
        String[] tables = {
                "clinics", "patients", "psychoanalysts", "clinic_memberships", "clinic_user_memberships",
                "appointments", "availability", "therapeutic_relationships", "medical_records",
                "medical_record_revisions", "medical_record_addendums", "receivables", "payments",
                "payment_allocations", "refunds", "refund_allocations", "credit_accounts", "credit_entries",
                "patient_packages", "patient_subscriptions", "subscription_cycles", "service_invoices",
                "fiscal_documents", "notifications", "notification_recipients", "notification_deliveries",
                "notification_preferences", "notification_provider_configurations", "domain_event_outbox",
                "data_subject_requests", "privacy_export_jobs", "privacy_contacts", "audit_logs",
                "security_events", "bank_accounts", "bank_transactions", "bank_reconciliation_allocations",
                "provider_settlements", "provider_settlement_items", "fiscal_providers", "fiscal_issuers",
                "fiscal_configurations", "service_invoice_items", "fiscal_operations",
                "service_invoice_tax_snapshots", "fiscal_events", "appointment_series",
                "availability_exceptions", "clinical_exports", "clinic_membership_periods", "financial_entities",
                "package_plans", "package_plan_versions", "package_plan_items", "patient_package_items",
                "patient_package_cancellations", "package_consumptions", "session_credit_entries",
                "receivable_cancellations", "receivable_adjustments", "subscription_payment_mandates",
                "subscription_charge_attempts", "bank_statement_imports", "notification_templates",
                "data_subject_request_decisions"
        };
        for (String table : tables) {
            try (PreparedStatement query = sql.getConnection().prepareStatement("""
                    SELECT c.relrowsecurity, c.relforcerowsecurity,
                           EXISTS (SELECT 1 FROM pg_policies p
                                   WHERE p.schemaname = 'public' AND p.tablename = c.relname
                                     AND (p.qual LIKE '%current_organization_id%'
                                          OR p.with_check LIKE '%current_organization_id%'))
                    FROM pg_class c
                    JOIN pg_namespace n ON n.oid = c.relnamespace
                    WHERE n.nspname = 'public' AND c.relname = ?
                    """)) {
                query.setString(1, table);
                try (ResultSet result = query.executeQuery()) {
                    assertThat(result.next()).as("critical table %s exists", table).isTrue();
                    assertThat(result.getBoolean(1)).as("RLS enabled for %s", table).isTrue();
                    assertThat(result.getBoolean(2)).as("RLS forced for %s", table).isTrue();
                    assertThat(result.getBoolean(3)).as("current-tenant policy exists for %s", table).isTrue();
                }
            }
        }
    }
}
