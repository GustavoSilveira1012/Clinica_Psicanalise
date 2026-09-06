package com.psicogest.psicogest.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationIntegrationTest
        extends PostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldApplyFlywayMigrations() {

        Integer migrations = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM flyway_schema_history
                        WHERE success = TRUE
                        """,
                Integer.class);

        assertThat(migrations)
                .isNotNull()
                .isGreaterThan(0);
    }

    @Test
    void shouldInstallBtreeGistExtension() {

        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM pg_extension
                        WHERE extname = 'btree_gist'
                        """,
                Integer.class);

        assertThat(count)
                .isEqualTo(1);
    }

    @Test
    void shouldCreateAppointmentExclusionConstraint() {

        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM pg_constraint
                        WHERE conname =
                            'ex_appointments_psychoanalyst_no_overlap'
                        """,
                Integer.class);

        assertThat(count)
                .isEqualTo(1);
    }

    @Test
    void shouldCreateMembershipPeriodOverlapConstraint() {

        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM pg_constraint
                        WHERE conname =
                            'ex_clinic_membership_period_no_overlap'
                        """,
                Integer.class);

        assertThat(count)
                .isEqualTo(1);
    }

    @Test
    void shouldCreateUniquePrimaryRelationshipIndex() {

        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM pg_indexes
                        WHERE indexname =
                            'ux_therapeutic_relationship_primary_patient'
                        """,
                Integer.class);

        assertThat(count)
                .isEqualTo(1);
    }
}