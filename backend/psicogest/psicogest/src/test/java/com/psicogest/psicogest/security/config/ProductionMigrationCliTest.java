package com.psicogest.psicogest.security.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Map;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class ProductionMigrationCliTest {
    private static final String URL =
            "jdbc:postgresql://db.synthetic.example:5432/psicogest?sslmode=verify-full";

    @Container
    static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>("postgres:17-alpine");

    @Test
    void rejectsMismatchedTargetBeforeConnecting() {
        Map<String, String> environment = approvedEnvironment();
        environment.put("MIGRATION_APPROVED_TARGET", "db.other.example:5432/psicogest");

        assertThatThrownBy(() -> ProductionMigrationCli.migrateFromEnvironment(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("target");
    }

    @Test
    void rejectsMissingConfirmationBeforeConnecting() {
        Map<String, String> environment = approvedEnvironment();
        environment.remove("MIGRATION_CONFIRMATION");

        assertThatThrownBy(() -> ProductionMigrationCli.migrateFromEnvironment(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("confirmation");
    }

    @Test
    void rejectsUnverifiedTlsBeforeConnecting() {
        Map<String, String> environment = approvedEnvironment();
        environment.put("DATABASE_URL", URL.replace("verify-full", "disable"));

        assertThatThrownBy(() -> ProductionMigrationCli.migrateFromEnvironment(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sslmode=verify-full");
    }

    @Test
    void oneShotFlywayConfigurationMigratesPostgres17AndIsIdempotent() {
        var flyway = ProductionMigrationCli.flyway(
                DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());

        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(106);
        flyway.validate();
        assertThat(flyway.info().current().getVersion().toString()).isEqualTo("96");
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }

    @Test
    void supabaseSeedFunctionBaselinesAtZeroAndAppliesEveryMigration() throws Exception {
        String url = newDatabase("supabase_seed");
        try (var connection = DriverManager.getConnection(
                url, DATABASE.getUsername(), DATABASE.getPassword());
                var statement = connection.createStatement()) {
            statement.execute("""
                    CREATE FUNCTION public.rls_auto_enable() RETURNS event_trigger
                    LANGUAGE plpgsql AS $$ BEGIN RETURN; END $$
                    """);
        }

        var flyway = ProductionMigrationCli.flyway(
                url, DATABASE.getUsername(), DATABASE.getPassword());
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(106);
        flyway.validate();
        assertThat(flyway.info().current().getVersion().toString()).isEqualTo("96");
        try (var connection = DriverManager.getConnection(
                url, DATABASE.getUsername(), DATABASE.getPassword());
                var statement = connection.createStatement();
                var rows = statement.executeQuery(
                        "SELECT version, type FROM public.flyway_schema_history ORDER BY installed_rank LIMIT 1")) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getString("version")).isEqualTo("0");
            assertThat(rows.getString("type")).isEqualTo("BASELINE");
        }
    }

    @Test
    void supabaseDataApiRolesCannotReadClinicalTablesAfterMigration() throws Exception {
        String url = newDatabase("data_api_grants");
        try (var connection = DriverManager.getConnection(
                url, DATABASE.getUsername(), DATABASE.getPassword());
                var statement = connection.createStatement()) {
            statement.execute("CREATE ROLE anon NOLOGIN");
            statement.execute("CREATE ROLE authenticated NOLOGIN");
            statement.execute("CREATE ROLE service_role NOLOGIN");
            statement.execute("""
                    ALTER DEFAULT PRIVILEGES IN SCHEMA public
                    GRANT ALL ON TABLES TO anon, authenticated, service_role
                    """);
            statement.execute("""
                    ALTER DEFAULT PRIVILEGES IN SCHEMA public
                    GRANT ALL ON SEQUENCES TO anon, authenticated, service_role
                    """);
        }

        var flyway = ProductionMigrationCli.flyway(
                url, DATABASE.getUsername(), DATABASE.getPassword());
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(106);
        try (var connection = DriverManager.getConnection(
                url, DATABASE.getUsername(), DATABASE.getPassword());
                var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE public.future_clinical_table (id bigint)");
            try (var rows = statement.executeQuery("""
                    SELECT has_table_privilege('anon', 'public.patients', 'SELECT'),
                           has_table_privilege('authenticated', 'public.patients', 'SELECT'),
                           has_table_privilege('service_role', 'public.patients', 'SELECT'),
                           has_table_privilege('anon', 'public.future_clinical_table', 'SELECT')
                    """)) {
                assertThat(rows.next()).isTrue();
                for (int column = 1; column <= 4; column++) {
                    assertThat(rows.getBoolean(column)).isFalse();
                }
            }
        }
    }

    @Test
    void rejectsUnexpectedObjectsInPublicInsteadOfBaseliningThem() throws Exception {
        String url = newDatabase("unexpected_public");
        try (var connection = DriverManager.getConnection(
                url, DATABASE.getUsername(), DATABASE.getPassword());
                var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE public.existing_record (id bigint PRIMARY KEY)");
        }

        var flyway = ProductionMigrationCli.flyway(
                url, DATABASE.getUsername(), DATABASE.getPassword());
        assertThatThrownBy(flyway::migrate)
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("non-empty schema");
    }

    private static String newDatabase(String databaseName) throws Exception {
        try (var connection = DriverManager.getConnection(
                DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
                var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + databaseName);
        }
        return DATABASE.getJdbcUrl().replaceFirst("/[^/]+$", "/" + databaseName);
    }

    private static Map<String, String> approvedEnvironment() {
        Map<String, String> environment = new HashMap<>();
        environment.put("DATABASE_URL", URL);
        environment.put("MIGRATION_DATABASE_USERNAME", "synthetic_migrator");
        environment.put("MIGRATION_DATABASE_PASSWORD", "test-only");
        environment.put("MIGRATION_APPROVED_TARGET", "db.synthetic.example:5432/psicogest");
        environment.put("MIGRATION_CONFIRMATION", "APPLY-PSICOGEST-MIGRATIONS");
        return environment;
    }
}
