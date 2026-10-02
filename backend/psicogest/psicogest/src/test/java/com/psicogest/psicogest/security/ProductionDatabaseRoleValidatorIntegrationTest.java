package com.psicogest.psicogest.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.psicogest.psicogest.security.config.ProductionDatabaseRoleValidator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class ProductionDatabaseRoleValidatorIntegrationTest {
    private static final String PASSWORD = "synthetic-only";

    @Container
    static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>("postgres:17-alpine");

    @BeforeAll
    static void createRoles() {
        JdbcTemplate admin = new JdbcTemplate(new DriverManagerDataSource(
                DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword()));
        admin.execute("REVOKE CREATE ON SCHEMA public FROM PUBLIC");
        for (String role : new String[]{"restricted_runtime", "schema_creator", "table_owner"}) {
            admin.execute("CREATE ROLE " + role + " LOGIN PASSWORD '" + PASSWORD + "' NOSUPERUSER NOBYPASSRLS");
            admin.execute("GRANT USAGE ON SCHEMA public TO " + role);
        }
        admin.execute("GRANT CREATE ON SCHEMA public TO schema_creator");
        admin.execute("CREATE TABLE owned_by_runtime (id integer PRIMARY KEY)");
        admin.execute("ALTER TABLE owned_by_runtime OWNER TO table_owner");
    }

    @Test
    void permitsRoleWithOnlySchemaUsage() {
        assertThatCode(() -> validator("restricted_runtime").afterSingletonsInstantiated())
                .doesNotThrowAnyException();
    }

    @Test
    void refusesRuntimeRoleWithSchemaCreatePermission() {
        assertThatThrownBy(() -> validator("schema_creator").afterSingletonsInstantiated())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("alterar o schema");
    }

    @Test
    void refusesRuntimeRoleOwningAnApplicationTable() {
        assertThatThrownBy(() -> validator("table_owner").afterSingletonsInstantiated())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("alterar o schema");
    }

    private static ProductionDatabaseRoleValidator validator(String username) {
        return new ProductionDatabaseRoleValidator(new JdbcTemplate(new DriverManagerDataSource(
                DATABASE.getJdbcUrl(), username, PASSWORD)));
    }
}
