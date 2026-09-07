package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.Appointment;
import com.psicogest.psicogest.model.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository
    extends JpaRepository<Appointment, Long> {

  Optional<Appointment> findByIdAndPsychoanalystId(
      Long appointmentId,
      Long psychoanalystId);

  List<Appointment> findByPsychoanalystIdOrderByScheduledStartAsc(
      Long psychoanalystId);

  List<Appointment> findByAppointmentSeriesIdOrderByOccurrenceNumberAsc(
      UUID appointmentSeriesId);

  List<Appointment> findByAppointmentSeriesIdAndOccurrenceNumberGreaterThanEqualOrderByOccurrenceNumberAsc(
      UUID appointmentSeriesId,
      Integer occurrenceNumber);

  boolean existsByPatientIdAndStatusInAndScheduledStartAfter(
      Long patientId,
      java.util.Collection<AppointmentStatus> statuses,
      LocalDateTime instant);

  boolean existsByPsychoanalystIdAndStatusInAndScheduledStartAfter(
      Long psychoanalystId,
      java.util.Collection<AppointmentStatus> statuses,
      LocalDateTime instant);

  List<Appointment> findByPatientIdOrderByScheduledStartDesc(
      Long patientId);

  Optional<Appointment> findByIdAndPsychoanalystIdAndAppointmentSeriesIsNotNull(
      Long appointmentId,
      Long psychoanalystId);

  @Query("""
        SELECT COUNT(a) > 0

        FROM Appointment a,
             ClinicUserMembership access

        WHERE a.patient.id = :patientId

          AND a.clinicMembership IS NOT NULL

          AND access.clinic.id =
                a.clinicMembership.clinic.id

          AND access.user.id = :userId

          AND access.accessRole =
                com.psicogest.model.enums.ClinicAccessRole.ADMIN

          AND access.status =
                com.psicogest.model.enums.ClinicUserMembershipStatus.ACTIVE
        """)
boolean existsPatientInClinicAdminScope1(
        @Param("patientId")
        Long patientId,

        @Param("userId")
        Long userId
);

boolean existsByIdAndPatientUserId(
        Long appointmentId,
        Long userId
);

boolean existsByIdAndPsychoanalystUserId(
        Long appointmentId,
        Long userId
);

@Query("""
        SELECT a.clinicMembership.clinic.id

        FROM Appointment a

        WHERE a.id = :appointmentId
          AND a.clinicMembership IS NOT NULL
        """)
Optional<Long> findClinicIdByAppointmentId(
        @Param("appointmentId")
        Long appointmentId
);

  boolean existsPatientInClinicAdminScope(Long patientId, Long userId);

  AbstractCollection<AppointmentStatus> findConflicts(Long psychoanalystId, LocalDateTime start, LocalDateTime end,
        EnumSet<AppointmentStatus> blockingStatuses, Long ignoredAppointmentId);
}