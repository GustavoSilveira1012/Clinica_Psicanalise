package com.psicogest.psicogest.infrastructure.health;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component("migrationsHealthIndicator")
@Profile("production")
public class MigrationReadinessHealthIndicator implements HealthIndicator {
    private final JdbcTemplate jdbc;
    private final String expectedVersion;

    public MigrationReadinessHealthIndicator(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.expectedVersion = MigrationReadiness.latestPackagedVersion();
    }

    @Override
    public Health health() {
        try {
            return MigrationReadiness.isCurrent(jdbc, expectedVersion)
                    ? Health.up().build() : Health.down().build();
        } catch (RuntimeException exception) {
            return Health.down().build();
        }
    }
}
