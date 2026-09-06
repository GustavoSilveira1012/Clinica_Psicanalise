package com.psicogest.psicogest.service;

import com.psicogest.psicogest.domain.appointment.AppointmentStateMachine;
import com.psicogest.psicogest.domain.appointment.AppointmentPersistenceExceptionTranslator;
import com.psicogest.psicogest.dto.appointment.AppointmentResponseDTO;
import com.psicogest.psicogest.dto.appointment.RecurringAppointmentCancelDTO;
import com.psicogest.psicogest.dto.appointment.RecurringAppointmentRescheduleDTO;
import com.psicogest.psicogest.exception.InvalidAppointmentSeriesException;
import com.psicogest.psicogest.exception.InvalidAppointmentTransitionException;
import com.psicogest.psicogest.exception.InvalidAvailabilityException;
import com.psicogest.psicogest.exception.ResourceNotFoundException;
import com.psicogest.psicogest.exception.ScheduleConflictException;
import com.psicogest.psicogest.model.entity.Appointment;
import com.psicogest.psicogest.model.entity.AppointmentSeries;
import com.psicogest.psicogest.model.enums.AppointmentSeriesStatus;
import com.psicogest.psicogest.model.enums.AppointmentStatus;
import com.psicogest.psicogest.model.enums.RecurrenceScope;
import com.psicogest.psicogest.repository.AppointmentRepository;
import com.psicogest.psicogest.repository.AppointmentSeriesRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppointmentSeriesOperationService {
    private final AppointmentRepository appointmentRepository;
    private final AppointmentSeriesRepository seriesRepository;
    private final AppointmentStateMachine stateMachine;
    private final ScheduleAvailabilityService scheduleAvailabilityService;
    private final AppointmentPersistenceExceptionTranslator
        persistenceExceptionTranslator;

    public AppointmentSeriesOperationService(
        AppointmentSeriesRepository appointmentSeriesRepository,
        AppointmentRepository appointmentRepository,
        AppointmentStateMachine stateMachine,
        ScheduleAvailabilityService scheduleAvailabilityService,
        AppointmentPersistenceExceptionTranslator
            persistenceExceptionTranslator
    ) {
        this.appointmentRepository = appointmentRepository;
        this.seriesRepository = appointmentSeriesRepository;
        this.stateMachine = stateMachine;
        this.scheduleAvailabilityService = scheduleAvailabilityService;
        this.persistenceExceptionTranslator = persistenceExceptionTranslator;
    }

        private Appointment findAppointment(
            Long psychoanalystId,
            Long appointmentId
        ) {
        return appointmentRepository
            .findByIdAndPsychoanalystId(
                appointmentId,
                psychoanalystId
            )
            .orElseThrow(() -> new ResourceNotFoundException(
                "Consulta não encontrada"
            ));
        }

        private AppointmentSeries requireSeries(Appointment appointment) {
        if (appointment.getAppointmentSeries() == null) {
            throw new InvalidAppointmentSeriesException(
                "Esta consulta não pertence a uma série recorrente"
            );
        }

        return appointment.getAppointmentSeries();
        }

        private boolean isMutable(Appointment appointment) {
        return appointment.getStatus() == AppointmentStatus.SCHEDULED
            || appointment.getStatus() == AppointmentStatus.CONFIRMED;
        }

        private List<Appointment> resolveAppointments(
            Appointment reference,
            RecurrenceScope scope
        ) {
        AppointmentSeries series = requireSeries(reference);

        return switch (scope) {
            case SINGLE -> List.of(reference);
            case THIS_AND_FUTURE -> appointmentRepository
                .findByAppointmentSeriesIdAndOccurrenceNumberGreaterThanEqualOrderByOccurrenceNumberAsc(
                    series.getId(),
                    reference.getOccurrenceNumber()
                );
            case ENTIRE_SERIES -> appointmentRepository
                .findByAppointmentSeriesIdOrderByOccurrenceNumberAsc(
                    series.getId()
                );
        };
        }

        @Transactional
        public List<AppointmentResponseDTO> cancel(
            Long psychoanalystId,
            Long appointmentId,
            RecurringAppointmentCancelDTO dto
        ) {
        Appointment reference = findAppointment(
            psychoanalystId,
            appointmentId
        );
        AppointmentSeries series = requireSeries(reference);

        List<Appointment> appointments = resolveAppointments(
            reference,
            dto.scope()
        );

        LocalDateTime now = LocalDateTime.now();

        List<Appointment> mutableAppointments = appointments
            .stream()
            .filter(this::isMutable)
            .filter(appointment -> now.isBefore(
                appointment.getScheduledStart()
            ))
            .toList();

        if (mutableAppointments.isEmpty()) {
            throw new InvalidAppointmentTransitionException(
                "Não existem consultas futuras elegíveis para cancelamento"
            );
        }

        for (Appointment appointment : mutableAppointments) {
            stateMachine.validateTransition(
                appointment.getStatus(),
                AppointmentStatus.CANCELLED
            );
            appointment.setStatus(AppointmentStatus.CANCELLED);
            appointment.setCancelledAt(now);
            appointment.setCancellationReason(dto.reason().trim());
        }

        appointmentRepository.saveAllAndFlush(mutableAppointments);

        if (dto.scope() == RecurrenceScope.THIS_AND_FUTURE
                || dto.scope() == RecurrenceScope.ENTIRE_SERIES) {
            series.setStatus(AppointmentSeriesStatus.CANCELLED);
            seriesRepository.save(series);
        }

        return mutableAppointments
            .stream()
            .map(this::toResponseDTO)
            .toList();
        }

            private Appointment rescheduleSingle(
                Appointment original,
                LocalDateTime newStart,
                LocalDateTime newEnd
            ) {
                validateNewPeriod(
                    original.getPsychoanalyst().getId(),
                    newStart,
                    newEnd,
                    original.getId()
                );

                stateMachine.validateTransition(
                    original.getStatus(),
                    AppointmentStatus.RESCHEDULED
                );

                LocalDateTime now = LocalDateTime.now();

                original.setStatus(AppointmentStatus.RESCHEDULED);
                original.setRescheduledAt(now);

                appointmentRepository.save(original);

                Appointment replacement = Appointment.builder()
                    .patient(original.getPatient())
                    .psychoanalyst(original.getPsychoanalyst())
                    .clinicMembership(original.getClinicMembership())
                    .appointmentSeries(original.getAppointmentSeries())
                    .occurrenceNumber(original.getOccurrenceNumber())
                    .originalAppointment(original)
                    .scheduledStart(newStart)
                    .scheduledEnd(newEnd)
                    .appointmentType(original.getAppointmentType())
                    .status(AppointmentStatus.SCHEDULED)
                    .build();

                return appointmentRepository.saveAndFlush(replacement);
            }

            private void validateNewPeriod(
                Long psychoanalystId,
                LocalDateTime newStart,
                LocalDateTime newEnd,
                Long ignoredAppointmentId
            ) {
                if (!newEnd.isAfter(newStart)) {
                    throw new InvalidAvailabilityException(
                        "Horário final deve ser posterior ao inicial"
                    );
                }

                if (!newStart.toLocalDate().equals(newEnd.toLocalDate())) {
                    throw new InvalidAvailabilityException(
                        "A consulta deve iniciar e terminar no mesmo dia"
                    );
                }

                if (!scheduleAvailabilityService.isWithinSchedule(
                    psychoanalystId,
                    newStart.toLocalDate(),
                    newStart.toLocalTime(),
                    newEnd.toLocalTime()
                )) {
                    throw new InvalidAvailabilityException(
                        "O novo horário está fora da disponibilidade"
                    );
                }

                boolean conflict = !appointmentRepository.findConflicts(
                    psychoanalystId,
                    newStart,
                    newEnd,
                    java.util.EnumSet.of(
                        AppointmentStatus.SCHEDULED,
                        AppointmentStatus.CONFIRMED
                    ),
                    ignoredAppointmentId
                ).isEmpty();

                if (conflict) {
                    throw new ScheduleConflictException(
                    "Existe conflito de agenda no novo horário"
                    );
                }
            }

            @Transactional
            public List<AppointmentResponseDTO> reschedule(
                Long psychoanalystId,
                Long appointmentId,
                RecurringAppointmentRescheduleDTO dto
            ) {
                Appointment reference = findAppointment(
                    psychoanalystId,
                    appointmentId
                );

                AppointmentSeries series = requireSeries(reference);

                return switch (dto.scope()) {
                    case SINGLE -> List.of(
                        toResponseDTO(
                            rescheduleSingle(
                                reference,
                                dto.scheduledStart(),
                                dto.scheduledEnd()
                            )
                        )
                    );
                    case THIS_AND_FUTURE -> rescheduleThisAndFuture(
                        reference,
                        dto
                    );
                    case ENTIRE_SERIES -> rescheduleEntireSeries(
                        reference,
                        dto
                    );
                };
            }

            private List<AppointmentResponseDTO> rescheduleEntireSeries(
                Appointment reference,
                RecurringAppointmentRescheduleDTO dto
            ) {
                AppointmentSeries series = requireSeries(reference);

                List<Appointment> allAppointments = appointmentRepository
                    .findByAppointmentSeriesIdOrderByOccurrenceNumberAsc(
                        series.getId()
                    );

                List<Appointment> futureMutable = allAppointments.stream()
                    .filter(this::isFutureMutable)
                    .toList();

                if (futureMutable.isEmpty()) {
                    throw new InvalidAppointmentSeriesException(
                        "A série não possui ocorrências futuras elegíveis"
                    );
                }

                Appointment firstFuture = futureMutable.get(0);

                return splitSeriesAndReschedule(
                    series,
                    firstFuture,
                    futureMutable,
                    dto
                );
            }

            private boolean isFutureMutable(Appointment appointment) {
                return isMutable(appointment)
                    && LocalDateTime.now().isBefore(
                        appointment.getScheduledStart()
                    );
            }

            private List<AppointmentResponseDTO> rescheduleThisAndFuture(
                Appointment reference,
                RecurringAppointmentRescheduleDTO dto
            ) {
                AppointmentSeries oldSeries = requireSeries(reference);

                List<Appointment> candidates = appointmentRepository
                    .findByAppointmentSeriesIdAndOccurrenceNumberGreaterThanEqualOrderByOccurrenceNumberAsc(
                        oldSeries.getId(),
                        reference.getOccurrenceNumber()
                    )
                    .stream()
                    .filter(this::isFutureMutable)
                    .toList();

                if (candidates.isEmpty()) {
                    throw new InvalidAppointmentSeriesException(
                        "Não existem ocorrências futuras elegíveis"
                    );
                }

                return splitSeriesAndReschedule(
                    oldSeries,
                    reference,
                    candidates,
                    dto
                );
            }

            private List<AppointmentResponseDTO> splitSeriesAndReschedule(
                AppointmentSeries oldSeries,
                Appointment reference,
                List<Appointment> candidates,
                RecurringAppointmentRescheduleDTO dto
            ) {
                if (candidates.size() < 2) {
                    throw new InvalidAppointmentSeriesException(
                        "A nova série deve possuir pelo menos duas consultas"
                    );
                }

                validateRequestedPeriod(
                    dto.scheduledStart(),
                    dto.scheduledEnd()
                );

                Duration newDuration = Duration.between(
                    dto.scheduledStart(),
                    dto.scheduledEnd()
                );

                List<NewOccurrence> newOccurrences = calculateNewOccurrences(
                    reference,
                    candidates,
                    dto.scheduledStart(),
                    newDuration
                );

                validateAllNewOccurrences(
                    oldSeries.getPsychoanalyst().getId(),
                    candidates,
                    newOccurrences
                );

                LocalDateTime now = LocalDateTime.now();

                AppointmentSeries newSeries = AppointmentSeries.builder()
                    .id(java.util.UUID.randomUUID())
                    .patient(oldSeries.getPatient())
                    .psychoanalyst(oldSeries.getPsychoanalyst())
                    .clinicMembership(oldSeries.getClinicMembership())
                    .previousSeries(oldSeries)
                    .frequency(oldSeries.getFrequency())
                    .recurrenceInterval(oldSeries.getRecurrenceInterval())
                    .dayOfWeek(dto.scheduledStart().getDayOfWeek())
                    .startTime(dto.scheduledStart().toLocalTime())
                    .durationMinutes(Math.toIntExact(newDuration.toMinutes()))
                    .startsOn(newOccurrences.get(0).start().toLocalDate())
                    .endsOn(newOccurrences.get(newOccurrences.size() - 1)
                        .start().toLocalDate())
                    .totalOccurrences(newOccurrences.size())
                    .status(AppointmentSeriesStatus.ACTIVE)
                    .build();

                AppointmentSeries savedNewSeries = seriesRepository.save(newSeries);

                oldSeries.setStatus(AppointmentSeriesStatus.SUPERSEDED);
                oldSeries.setSupersededAt(now);
                oldSeries.setSupersededFrom(
                    reference.getScheduledStart().toLocalDate()
                );
                seriesRepository.save(oldSeries);

                for (Appointment oldAppointment : candidates) {
                    stateMachine.validateTransition(
                        oldAppointment.getStatus(),
                        AppointmentStatus.RESCHEDULED
                    );
                    oldAppointment.setStatus(AppointmentStatus.RESCHEDULED);
                    oldAppointment.setRescheduledAt(now);
                }

                appointmentRepository.saveAll(candidates);

                List<Appointment> replacements = new java.util.ArrayList<>();

                for (int i = 0; i < candidates.size(); i++) {
                    Appointment oldAppointment = candidates.get(i);
                    NewOccurrence occurrence = newOccurrences.get(i);

                    replacements.add(Appointment.builder()
                        .patient(oldAppointment.getPatient())
                        .psychoanalyst(oldAppointment.getPsychoanalyst())
                        .clinicMembership(oldAppointment.getClinicMembership())
                        .appointmentSeries(savedNewSeries)
                        .occurrenceNumber(i + 1)
                        .originalAppointment(oldAppointment)
                        .scheduledStart(occurrence.start())
                        .scheduledEnd(occurrence.end())
                        .appointmentType(oldAppointment.getAppointmentType())
                        .status(AppointmentStatus.SCHEDULED)
                        .build());
                }

                List<Appointment> saved = saveAllSafely(replacements);

                return saved.stream()
                    .map(this::toResponseDTO)
                    .toList();
            }

            private void validateRequestedPeriod(
                LocalDateTime start,
                LocalDateTime end
            ) {
                if (start == null || end == null || !end.isAfter(start)) {
                    throw new InvalidAvailabilityException(
                        "Duração da consulta é inválida"
                    );
                }
            }

            private List<NewOccurrence> calculateNewOccurrences(
                Appointment reference,
                List<Appointment> candidates,
                LocalDateTime requestedStart,
                Duration duration
            ) {
                List<NewOccurrence> result = new java.util.ArrayList<>();
                LocalDateTime originalReferenceStart =
                    reference.getScheduledStart();

                for (Appointment appointment : candidates) {
                    Duration offset = Duration.between(
                        originalReferenceStart,
                        appointment.getScheduledStart()
                    );
                    LocalDateTime newStart = requestedStart.plus(offset);
                    result.add(new NewOccurrence(
                        newStart,
                        newStart.plus(duration)
                    ));
                }

                return result;
            }

            private void validateAllNewOccurrences(
                Long psychoanalystId,
                List<Appointment> oldAppointments,
                List<NewOccurrence> newOccurrences
            ) {
                for (int i = 0; i < newOccurrences.size(); i++) {
                    NewOccurrence occurrence = newOccurrences.get(i);
                    validateNewPeriod(
                        psychoanalystId,
                        occurrence.start(),
                        occurrence.end(),
                        oldAppointments.get(i).getId()
                    );
                }
            }

            private List<Appointment> saveAllSafely(
                List<Appointment> appointments
            ) {
                try {
                    return appointmentRepository.saveAllAndFlush(appointments);
                } catch (DataIntegrityViolationException exception) {
                    throw persistenceExceptionTranslator.translate(exception);
                }
            }

            private record NewOccurrence(
                LocalDateTime start,
                LocalDateTime end
            ) {
            }

            private List<AppointmentResponseDTO> rescheduleMultiple(
                AppointmentSeries series,
                Appointment reference,
                boolean entireSeries,
                RecurringAppointmentRescheduleDTO dto
            ) {
                List<Appointment> appointments;

                if (entireSeries) {
                    appointments = appointmentRepository
                        .findByAppointmentSeriesIdOrderByOccurrenceNumberAsc(
                            series.getId()
                        );
                } else {
                    appointments = appointmentRepository
                        .findByAppointmentSeriesIdAndOccurrenceNumberGreaterThanEqualOrderByOccurrenceNumberAsc(
                            series.getId(),
                            reference.getOccurrenceNumber()
                        );
                }

                appointments = appointments.stream()
                    .filter(this::isMutable)
                    .filter(appointment -> appointment.getScheduledStart()
                        .isAfter(LocalDateTime.now()))
                    .toList();

                if (appointments.isEmpty()) {
                    throw new InvalidAppointmentSeriesException(
                        "Não existem ocorrências futuras elegíveis"
                    );
                }

                Duration newDuration = Duration.between(
                    dto.scheduledStart(),
                    dto.scheduledEnd()
                );

                if (newDuration.isZero() || newDuration.isNegative()) {
                    throw new InvalidAvailabilityException(
                        "Duração da consulta é inválida"
                    );
                }

                LocalDateTime firstOldStart = reference.getScheduledStart();
                LocalDateTime firstNewStart = dto.scheduledStart();

                for (Appointment current : appointments) {
                    Duration displacement = Duration.between(
                        firstOldStart,
                        current.getScheduledStart()
                    );

                    LocalDateTime newStart = firstNewStart.plus(displacement);
                    LocalDateTime newEnd = newStart.plus(newDuration);

                    validateNewPeriod(
                        current.getPsychoanalyst().getId(),
                        newStart,
                        newEnd,
                        current.getId()
                    );
                }

                LocalDateTime now = LocalDateTime.now();
                List<Appointment> replacements = new java.util.ArrayList<>();

                for (Appointment current : appointments) {
                    Duration displacement = Duration.between(
                        firstOldStart,
                        current.getScheduledStart()
                    );

                    LocalDateTime newStart = firstNewStart.plus(displacement);
                    LocalDateTime newEnd = newStart.plus(newDuration);

                    stateMachine.validateTransition(
                        current.getStatus(),
                        AppointmentStatus.RESCHEDULED
                    );

                    current.setStatus(AppointmentStatus.RESCHEDULED);
                    current.setRescheduledAt(now);

                    replacements.add(
                        Appointment.builder()
                            .patient(current.getPatient())
                            .psychoanalyst(current.getPsychoanalyst())
                            .clinicMembership(current.getClinicMembership())
                            .appointmentSeries(series)
                            .occurrenceNumber(current.getOccurrenceNumber())
                            .originalAppointment(current)
                            .scheduledStart(newStart)
                            .scheduledEnd(newEnd)
                            .appointmentType(current.getAppointmentType())
                            .status(AppointmentStatus.SCHEDULED)
                            .build()
                    );
                }

                appointmentRepository.saveAll(appointments);
                List<Appointment> saved = appointmentRepository
                    .saveAllAndFlush(replacements);

                updateSeriesDefinition(series, saved);

                return saved.stream()
                    .map(this::toResponseDTO)
                    .toList();
            }

            private void updateSeriesDefinition(
                AppointmentSeries series,
                List<Appointment> appointments
            ) {
                if (appointments.isEmpty()) {
                    return;
                }

                Appointment first = appointments.get(0);
                Appointment last = appointments.get(appointments.size() - 1);

                series.setStartsOn(first.getScheduledStart().toLocalDate());
                series.setStartTime(first.getScheduledStart().toLocalTime());
                series.setDayOfWeek(first.getScheduledStart().getDayOfWeek());
                series.setEndsOn(last.getScheduledStart().toLocalDate());
                series.setTotalOccurrences(appointments.size());

                seriesRepository.save(series);
            }

        private AppointmentResponseDTO toResponseDTO(Appointment appointment) {
        var membership = appointment.getClinicMembership();

        return new AppointmentResponseDTO(
            appointment.getId(),
            appointment.getPatient().getId(),
            appointment.getPatient().getUser().getName(),
            appointment.getPsychoanalyst().getId(),
            appointment.getPsychoanalyst().getUser().getName(),
            membership != null ? membership.getId() : null,
            membership != null ? membership.getClinic().getId() : null,
            membership != null ? membership.getClinic().getName() : null,
            appointment.getOriginalAppointment() != null
                ? appointment.getOriginalAppointment().getId()
                : null,
            appointment.getAppointmentSeries() != null
                ? appointment.getAppointmentSeries().getId()
                : null,
            appointment.getOccurrenceNumber(),
            appointment.getScheduledStart(),
            appointment.getScheduledEnd(),
            appointment.getStatus(),
            appointment.getAppointmentType(),
            appointment.getCancellationReason(),
            appointment.getCancelledAt(),
            appointment.getConfirmedAt(),
            appointment.getCompletedAt(),
            appointment.getNoShowAt(),
            appointment.getRescheduledAt()
        );
        }
    }
