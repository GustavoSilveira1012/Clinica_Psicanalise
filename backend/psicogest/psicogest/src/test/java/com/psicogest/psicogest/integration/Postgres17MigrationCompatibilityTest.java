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

        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(104);
        flyway.validate();
        assertThat(flyway.info().current().getVersion().toString()).isEqualTo("94");
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
    }
}
