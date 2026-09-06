package com.psicogest.psicogest.service;

import com.psicogest.psicogest.domain.appointment.AppointmentStateMachine;
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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppointmentSeriesOperationService {
    private final AppointmentRepository appointmentRepository;
    private final AppointmentSeriesRepository seriesRepository;
    private final AppointmentStateMachine stateMachine;
    private final ScheduleAvailabilityService scheduleAvailabilityService;

    public AppointmentSeriesOperationService(
        AppointmentSeriesRepository appointmentSeriesRepository,
        AppointmentRepository appointmentRepository,
        AppointmentStateMachine stateMachine,
        ScheduleAvailabilityService scheduleAvailabilityService
    ) {
        this.appointmentRepository = appointmentRepository;
        this.seriesRepository = appointmentSeriesRepository;
        this.stateMachine = stateMachine;
        this.scheduleAvailabilityService = scheduleAvailabilityService;
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
        updateSeriesStatusIfNecessary(series);

        return mutableAppointments
            .stream()
            .map(this::toResponseDTO)
            .toList();
        }

        private void updateSeriesStatusIfNecessary(AppointmentSeries series) {
        List<Appointment> appointments = appointmentRepository
            .findByAppointmentSeriesIdOrderByOccurrenceNumberAsc(
                series.getId()
            );

        boolean hasFutureActive = appointments.stream().anyMatch(
            appointment -> isMutable(appointment)
                && appointment.getScheduledStart().isAfter(
                    LocalDateTime.now()
                )
        );

        if (!hasFutureActive) {
            boolean allCancelled = appointments.stream().allMatch(
                appointment -> appointment.getStatus()
                    == AppointmentStatus.CANCELLED
                    || appointment.getStatus()
                    == AppointmentStatus.RESCHEDULED
            );

            series.setStatus(allCancelled
                ? AppointmentSeriesStatus.CANCELLED
                : AppointmentSeriesStatus.COMPLETED);
            seriesRepository.save(series);
        }
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
                    case THIS_AND_FUTURE -> rescheduleMultiple(
                        series,
                        reference,
                        false,
                        dto
                    );
                    case ENTIRE_SERIES -> rescheduleMultiple(
                        series,
                        reference,
                        true,
                        dto
                    );
                };
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
            appointment.getRecurringGroupId(),
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
