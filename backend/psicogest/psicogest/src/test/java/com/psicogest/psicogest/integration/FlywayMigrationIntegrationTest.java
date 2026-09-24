package com.psicogest.psicogest.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;
import com.psicogest.psicogest.model.entity.*;
import com.psicogest.psicogest.model.enums.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationIntegrationTest
        extends PostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired private EntityManager entityManager;

    @Test
    @Transactional
    void shouldPersistRealJpaMappingsAndCancellationHistory() {
        var user = User.builder().name("Synthetic mapping test")
                .email(UUID.randomUUID() + "@example.invalid").passwordHash("not-a-usable-hash")
                .role(UserRole.CLINIC_ADMIN).active(true).build();
        entityManager.persist(user);
        UUID organization = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO organizations(id,name,slug,type,owner_user_id,created_at,updated_at)
                VALUES (?, 'Synthetic mappings', ?, 'CLINIC', ?, now(), now())
                """, organization, "mapping-" + organization, user.getId());
        jdbcTemplate.queryForObject("SELECT set_config('app.organization_id', ?, true)", String.class, organization.toString());
        var clinic = Clinic.builder().name("Synthetic mapping clinic").organizationId(organization).build();
        entityManager.persist(clinic);
        var patient = Patient.builder().user(user).build();
        entityManager.persist(patient);
        var professional = Psychoanalyst.builder().user(user).build();
        entityManager.persist(professional);
        var membership = ClinicMembership.builder().clinic(clinic).psychoanalyst(professional)
                .status(MembershipStatus.ACTIVE).build();
        entityManager.persist(membership);
        var appointment = Appointment.builder().patient(patient).psychoanalyst(professional)
                .clinicMembership(membership).appointmentType(AppointmentType.ONLINE)
                .scheduledStart(LocalDateTime.of(2030, 1, 10, 10, 0))
                .scheduledEnd(LocalDateTime.of(2030, 1, 10, 11, 0))
                .status(AppointmentStatus.SCHEDULED).build();
        entityManager.persist(appointment);
        entityManager.flush();
        appointment.setStatus(AppointmentStatus.RESCHEDULED);
        appointment.setRescheduledAt(LocalDateTime.of(2030, 1, 1, 12, 0));
        Instant now = Instant.parse("2030-01-01T12:00:00Z");
        var receivable = Receivable.builder().id(UUID.randomUUID()).patient(patient).clinic(clinic)
                .appointment(appointment).description("Synthetic receivable").grossAmount(new BigDecimal("100.00"))
                .discountAmount(BigDecimal.ZERO).netAmount(new BigDecimal("100.00")).currency("BRL")
                .status(Receivable.ReceivableStatus.OPEN).dueDate(LocalDate.of(2030, 1, 10))
                .createdAt(now).updatedAt(now).build();
        entityManager.persist(receivable);
        var adjustment = new ReceivableAdjustment(receivable, ReceivableAdjustmentDirection.DECREASE,
                BigDecimal.TEN, "SYNTHETIC_CORRECTION", ReceivableAdjustmentType.BILLING_CORRECTION, user);
        entityManager.persist(adjustment);
        var cancellation = ReceivableCancellation.builder().id(UUID.randomUUID()).receivable(receivable)
                .createdBy(user).createdAt(now).requestedAt(now).updatedAt(now)
                .previousReceivableStatus(Receivable.ReceivableStatus.OPEN)
                .reason(ReceivableCancellationReason.ADMINISTRATIVE_ERROR).mode(ReceivableCancellationMode.REFUND)
                .status(ReceivableCancellation.ReceivableCancellationStatus.PENDING).build();
        entityManager.persist(cancellation);
        var credit = SessionCreditEntry.builder().id(UUID.randomUUID()).patient(patient)
                .direction(SessionCreditDirection.CREDIT).entryType(SessionCreditEntryType.MANUAL_ADJUSTMENT)
                .sessionCount(2L).reason("SYNTHETIC_CREDIT").createdAt(now).build();
        entityManager.persist(credit);
        entityManager.flush();
        entityManager.clear();
        assertThat(entityManager.find(Appointment.class, appointment.getId()).getStatus()).isEqualTo(AppointmentStatus.RESCHEDULED);
        assertThat(entityManager.find(ReceivableCancellation.class, cancellation.getId()).getStatus())
                .isEqualTo(ReceivableCancellation.ReceivableCancellationStatus.PENDING);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM receivable_cancellations WHERE id=?", String.class, cancellation.getId()))
                .isEqualTo("SETTLING");
        assertThat(entityManager.find(ReceivableAdjustment.class, adjustment.getId()).getDirection())
                .isEqualTo(ReceivableAdjustmentDirection.DECREASE);
        assertThat(entityManager.find(SessionCreditEntry.class, credit.getId()).getSessionCount()).isEqualTo(2L);
    }

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
