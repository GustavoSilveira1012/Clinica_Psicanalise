package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.Appointment;
import com.psicogest.psicogest.model.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
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
      SELECT a
      FROM Appointment a
      WHERE a.psychoanalyst.id = :psychoanalystId

        AND a.status IN :statuses

        AND a.scheduledStart < :requestedEnd

        AND a.scheduledEnd > :requestedStart

        AND (:ignoredAppointmentId IS NULL
             OR a.id <> :ignoredAppointmentId)
      """)
  List<Appointment> findConflicts(
      @Param("psychoanalystId") Long psychoanalystId,

      @Param("requestedStart") LocalDateTime requestedStart,

      @Param("requestedEnd") LocalDateTime requestedEnd,

      @Param("statuses") Collection<AppointmentStatus> statuses,

      @Param("ignoredAppointmentId") Long ignoredAppointmentId);
}