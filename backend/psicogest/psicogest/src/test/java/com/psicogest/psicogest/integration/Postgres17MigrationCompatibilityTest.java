package com.psicogest.psicogest.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** The free Supabase homologation project currently runs PostgreSQL 17. */
@Testcontainers(disabledWithoutDocker = true)
class Postgres17MigrationCompatibilityTest {
    @Container
    static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>("postgres:17-alpine");

    @Test
    void bootstrapsCanonicalFlywayHistoryOnPostgres17() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword())
                .locations("classpath:bd/migration")
                .baselineOnMigrate(false)
                .cleanDisabled(true)
                .load();

        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(106);
        flyway.validate();
        assertThat(flyway.info().current().getVersion().toString()).isEqualTo("96");
        assertThat(flyway.migrate().migrationsExecuted).isZero();

        try (var connection = DriverManager.getConnection(
                DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
             var statement = connection.createStatement();
             var result = statement.executeQuery("""
                     SELECT count(*) FROM pg_extension WHERE extname = 'btree_gist'
                     """)) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isEqualTo(1);
        }

        try (var connection = DriverManager.getConnection(
                DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
             var statement = connection.createStatement();
             var result = statement.executeQuery("""
                     SELECT count(*) FROM pg_proc p
                     JOIN pg_namespace n ON n.oid = p.pronamespace
                     WHERE n.nspname IN ('public', 'app')
                       AND p.proconfig IS NULL
                       AND NOT EXISTS (
                           SELECT 1 FROM pg_depend d
                           WHERE d.objid = p.oid AND d.deptype = 'e'
                       )
                     """)) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isZero();
        }

        try (var connection = DriverManager.getConnection(
                DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
             var statement = connection.createStatement();
             var result = statement.executeQuery("""
                     SELECT count(*) FROM pg_policies
                     WHERE schemaname = 'public'
                       AND policyname = 'users_runtime_auth'
                       AND 'psicogest_runtime' = ANY(roles)
                     """)) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isEqualTo(1);
        }
    }
}
