package com.psicogest.psicogest.security.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
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

        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(103);
        flyway.validate();
        assertThat(flyway.info().current().getVersion().toString()).isEqualTo("93");
        assertThat(flyway.migrate().migrationsExecuted).isZero();
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
