package com.psicogest.psicogest.infrastructure.health;

import java.io.IOException;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MigrationReadinessHealthIndicatorTest {

    @Test
    void productionReadinessGroupIncludesMigrations() throws IOException {
        Properties properties = new Properties();
        try (var stream = getClass().getResourceAsStream("/application-production.properties")) {
            assertThat(stream).isNotNull();
            properties.load(stream);
        }
        assertThat(properties.getProperty("management.endpoint.health.group.readiness.include"))
                .contains("migrations");
    }

    @Test
    void indicatorIsDownWhenSchemaIsBehind() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Boolean.class))).thenReturn(true);
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), eq("92"))).thenReturn(false);

        assertThat(new MigrationReadinessHealthIndicator(jdbc).health().getStatus().getCode())
                .isEqualTo("DOWN");
    }

    @Test
    void indicatorIsUpWhenCurrentMigrationSucceeded() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Boolean.class))).thenReturn(true);
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), eq("92"))).thenReturn(true);

        assertThat(new MigrationReadinessHealthIndicator(jdbc).health().getStatus().getCode())
                .isEqualTo("UP");
    }
}
