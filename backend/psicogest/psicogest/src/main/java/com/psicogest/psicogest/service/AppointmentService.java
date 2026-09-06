package com.psicogest.psicogest.service;

import com.psicogest.psicogest.dto.appointment.*;
import com.psicogest.psicogest.domain.appointment.AppointmentPersistenceExceptionTranslator;
import com.psicogest.psicogest.domain.appointment.AppointmentStateMachine;
import com.psicogest.psicogest.exception.InvalidAvailabilityException;
import com.psicogest.psicogest.exception.InvalidAppointmentTransitionException;
import com.psicogest.psicogest.exception.ResourceNotFoundException;
import com.psicogest.psicogest.exception.ScheduleConflictException;
import com.psicogest.psicogest.model.entity.*;
import com.psicogest.psicogest.model.enums.AppointmentStatus;
import com.psicogest.psicogest.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
public class AppointmentService {

    private static final EnumSet<AppointmentStatus>
            BLOCKING_STATUSES =
            EnumSet.of(
                    AppointmentStatus.SCHEDULED,
                    AppointmentStatus.CONFIRMED
            );

    private final AppointmentRepository appointmentRepository;

    private final AppointmentPersistenceExceptionTranslator
    persistenceExceptionTranslator;

    private final PatientRepository patientRepository;

    private final PsychoanalystRepository psychoanalystRepository;

    private final ClinicMembershipRepository membershipRepository;

        private final ClinicMembershipPeriodRepository membershipPeriodRepository;

    private final ScheduleAvailabilityService scheduleAvailabilityService;

    private final AppointmentStateMachine stateMachine;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            PatientRepository patientRepository,
            PsychoanalystRepository psychoanalystRepository,
            ClinicMembershipRepository membershipRepository,
            ClinicMembershipPeriodRepository membershipPeriodRepository,
            ScheduleAvailabilityService scheduleAvailabilityService,
            AppointmentStateMachine stateMachine,
            AppointmentPersistenceExceptionTranslator
                    persistenceExceptionTranslator
    ) {

        this.appointmentRepository =
                appointmentRepository;

        this.patientRepository =
                patientRepository;

        this.psychoanalystRepository =
                psychoanalystRepository;

        this.membershipRepository =
                membershipRepository;

        this.membershipPeriodRepository =
                membershipPeriodRepository;

        this.scheduleAvailabilityService =
                scheduleAvailabilityService;

        this.stateMachine =
                stateMachine;

        this.persistenceExceptionTranslator =
                persistenceExceptionTranslator;
    }

    @Transactional
    public AppointmentResponseDTO create(
            Long psychoanalystId,
            AppointmentCreateDTO dto
    ) {

        validateDateTime(
                dto.scheduledStart(),
                dto.scheduledEnd()
        );

        Psychoanalyst psychoanalyst =
                findPsychoanalyst(
                        psychoanalystId
                );

        Patient patient =
                findPatient(
                        dto.patientId()
                );

        ClinicMembership membership =
                resolveMembership(
                        psychoanalystId,
                        dto.clinicMembershipId(),
                        dto.scheduledStart()
                );

        validateSchedule(
                psychoanalystId,
                dto.scheduledStart(),
                dto.scheduledEnd()
        );

        validateAppointmentConflict(
                psychoanalystId,
                dto.scheduledStart(),
                dto.scheduledEnd(),
                null
        );

        Appointment appointment =
                Appointment.builder()
                        .patient(patient)
                        .psychoanalyst(psychoanalyst)
                        .clinicMembership(membership)
                        .scheduledStart(
                                dto.scheduledStart()
                        )
                        .scheduledEnd(
                                dto.scheduledEnd()
                        )
                        .appointmentType(
                                dto.appointmentType()
                        )
                        .status(
                                AppointmentStatus.SCHEDULED
                        )
                        .build();

        return toResponseDTO(
                saveSafely(
                        appointment
                )
        );
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponseDTO>
    findByPsychoanalyst(
            Long psychoanalystId
    ) {

        findPsychoanalyst(
                psychoanalystId
        );

        return appointmentRepository
                .findByPsychoanalystIdOrderByScheduledStartAsc(
                        psychoanalystId
                )
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public AppointmentResponseDTO findById(
            Long psychoanalystId,
            Long appointmentId
    ) {

        return toResponseDTO(
                findAppointment(
                        psychoanalystId,
                        appointmentId
                )
        );
    }

    @Transactional
    public AppointmentResponseDTO cancel(
            Long psychoanalystId,
            Long appointmentId,
            AppointmentCancelDTO dto
    ) {

        Appointment appointment =
                findAppointment(
                        psychoanalystId,
                        appointmentId
                );

        LocalDateTime now = LocalDateTime.now();

        if (!now.isBefore(appointment.getScheduledStart())) {
            throw new InvalidAppointmentTransitionException(
                    "A consulta não pode ser cancelada após o horário de início"
            );
        }

        transition(
                appointment,
                AppointmentStatus.CANCELLED
        );

        appointment.setCancelledAt(now);

        appointment.setCancellationReason(
                dto.reason().trim()
        );

        return toResponseDTO(
                saveSafely(
                        appointment
                )
        );
    }

    @Transactional
    public AppointmentResponseDTO confirm(
            Long psychoanalystId,
            Long appointmentId
    ) {

        Appointment appointment =
                findAppointment(
                        psychoanalystId,
                        appointmentId
                );

        LocalDateTime now = LocalDateTime.now();

        if (!now.isBefore(appointment.getScheduledStart())) {
            throw new InvalidAppointmentTransitionException(
                    "Não é possível confirmar uma consulta que já iniciou"
            );
        }

        transition(
                appointment,
                AppointmentStatus.CONFIRMED
        );

        appointment.setConfirmedAt(now);

        return toResponseDTO(
                saveSafely(appointment)
        );
    }

    @Transactional
    public AppointmentResponseDTO complete(
            Long psychoanalystId,
            Long appointmentId
    ) {

        Appointment appointment =
                findAppointment(
                        psychoanalystId,
                        appointmentId
                );

        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(appointment.getScheduledEnd())) {
            throw new InvalidAppointmentTransitionException(
                    "A consulta só pode ser concluída após o horário previsto de término"
            );
        }

        transition(
                appointment,
                AppointmentStatus.COMPLETED
        );

        appointment.setCompletedAt(now);

        return toResponseDTO(
                saveSafely(appointment)
        );
    }

    @Transactional
    public AppointmentResponseDTO markNoShow(
            Long psychoanalystId,
            Long appointmentId
    ) {

        Appointment appointment =
                findAppointment(
                        psychoanalystId,
                        appointmentId
                );

        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(appointment.getScheduledEnd())) {
            throw new InvalidAppointmentTransitionException(
                    "A ausência só pode ser registrada após o término previsto da consulta"
            );
        }

        transition(
                appointment,
                AppointmentStatus.NO_SHOW
        );

        appointment.setNoShowAt(now);

        return toResponseDTO(
                saveSafely(appointment)
        );
    }

    @Transactional
    public AppointmentResponseDTO reschedule(
            Long psychoanalystId,
            Long appointmentId,
            AppointmentRescheduleDTO dto
    ) {

        Appointment original =
                findAppointment(
                        psychoanalystId,
                        appointmentId
                );

        LocalDateTime now = LocalDateTime.now();

        if (!now.isBefore(original.getScheduledStart())) {
            throw new InvalidAppointmentTransitionException(
                    "Não é possível reagendar uma consulta que já iniciou"
            );
        }

        validateDateTime(
                dto.scheduledStart(),
                dto.scheduledEnd()
        );

        validateSchedule(
                psychoanalystId,
                dto.scheduledStart(),
                dto.scheduledEnd()
        );

        validateAppointmentConflict(
                psychoanalystId,
                dto.scheduledStart(),
                dto.scheduledEnd(),
                original.getId()
        );

        /*
         * Nunca sobrescrevemos o registro original.
         */
        transition(
                original,
                AppointmentStatus.RESCHEDULED
        );

        original.setRescheduledAt(now);

        appointmentRepository.save(
                original
        );

        Appointment newAppointment =
                Appointment.builder()
                        .patient(
                                original.getPatient()
                        )
                        .psychoanalyst(
                                original.getPsychoanalyst()
                        )
                        .clinicMembership(
                                original.getClinicMembership()
                        )
                        .originalAppointment(
                                original
                        )
                        .scheduledStart(
                                dto.scheduledStart()
                        )
                        .scheduledEnd(
                                dto.scheduledEnd()
                        )
                        .appointmentType(
                                original.getAppointmentType()
                        )
                        .status(
                                AppointmentStatus.SCHEDULED
                        )
                        .build();

        Appointment saved =
                saveSafely(newAppointment);

        return toResponseDTO(saved);
    }

    @Transactional
    public List<AppointmentResponseDTO>
    createWeeklyRecurring(
            Long psychoanalystId,
            RecurringAppointmentCreateDTO dto
    ) {

        validateDateTime(
                dto.firstScheduledStart(),
                dto.firstScheduledEnd()
        );

        Psychoanalyst psychoanalyst =
                findPsychoanalyst(
                        psychoanalystId
                );

        Patient patient =
                findPatient(
                        dto.patientId()
                );

        ClinicMembership membership =
                resolveMembership(
                        psychoanalystId,
                        dto.clinicMembershipId(),
                        dto.firstScheduledStart()
                );

        /*
         * Primeiro validamos TODAS as ocorrências.
         *
         * Só depois gravamos.
         */
        for (
                int i = 0;
                i < dto.occurrences();
                i++
        ) {

            LocalDateTime start =
                    dto.firstScheduledStart()
                            .plusWeeks(i);

            LocalDateTime end =
                    dto.firstScheduledEnd()
                            .plusWeeks(i);

            validateSchedule(
                    psychoanalystId,
                    start,
                    end
            );

            validateAppointmentConflict(
                    psychoanalystId,
                    start,
                    end,
                    null
            );
        }

        List<Appointment> appointments =
                java.util.stream.IntStream
                        .range(
                                0,
                                dto.occurrences()
                        )
                        .mapToObj(i -> {

                            LocalDateTime start =
                                    dto.firstScheduledStart()
                                            .plusWeeks(i);

                            LocalDateTime end =
                                    dto.firstScheduledEnd()
                                            .plusWeeks(i);

                            return Appointment.builder()
                                    .patient(patient)
                                    .psychoanalyst(
                                            psychoanalyst
                                    )
                                    .clinicMembership(
                                            membership
                                    )
                                    .scheduledStart(
                                            start
                                    )
                                    .scheduledEnd(
                                            end
                                    )
                                    .appointmentType(
                                            dto.appointmentType()
                                    )
                                    .status(
                                            AppointmentStatus.SCHEDULED
                                    )
                                    .build();

                        })
                        .toList();

        return saveAllSafely(
                appointments
        )
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    private void validateSchedule(
            Long psychoanalystId,
            LocalDateTime start,
            LocalDateTime end
    ) {

        boolean available =
                scheduleAvailabilityService
                        .isWithinSchedule(
                                psychoanalystId,
                                start.toLocalDate(),
                                start.toLocalTime(),
                                end.toLocalTime()
                        );

        if (!available) {

            throw new InvalidAvailabilityException(
                    "O horário solicitado está fora da disponibilidade do psicanalista"
            );
        }
    }

    private void validateAppointmentConflict(
            Long psychoanalystId,
            LocalDateTime start,
            LocalDateTime end,
            Long ignoredAppointmentId
    ) {

        boolean conflict =
                !appointmentRepository
                        .findConflicts(
                                psychoanalystId,
                                start,
                                end,
                                BLOCKING_STATUSES,
                                ignoredAppointmentId
                        )
                        .isEmpty();

        if (conflict) {

            throw new ScheduleConflictException(
                    "Já existe uma consulta neste intervalo"
            );
        }
    }

        private Appointment saveSafely(
                        Appointment appointment
        ) {

                try {

                        return appointmentRepository
                                        .saveAndFlush(appointment);

                } catch (DataIntegrityViolationException exception) {

                        throw persistenceExceptionTranslator
                                        .translate(exception);
                }
        }

        private List<Appointment> saveAllSafely(
                        List<Appointment> appointments
        ) {

                try {

                        return appointmentRepository
                                        .saveAllAndFlush(appointments);

                } catch (DataIntegrityViolationException exception) {

                        throw persistenceExceptionTranslator
                                        .translate(exception);
                }
        }

        private void transition(
                        Appointment appointment,
                        AppointmentStatus target
        ) {

                stateMachine.validateTransition(
                                appointment.getStatus(),
                                target
                );

                appointment.setStatus(target);
        }

    private ClinicMembership resolveMembership(
            Long psychoanalystId,
            Long membershipId,
            LocalDateTime appointmentStart
    ) {

        if (membershipId == null) {
            return null;
        }

        ClinicMembership membership =
                membershipRepository
                        .findByIdAndPsychoanalystId(
                                membershipId,
                                psychoanalystId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Vínculo com clínica não encontrado"
                                )
                        );

        boolean validPeriod = membershipPeriodRepository.isActiveAt(
                membershipId,
                appointmentStart
        );

        if (!validPeriod) {

            throw new ScheduleConflictException(
                    "O psicanalista não possui vínculo ativo com esta clínica na data da consulta"
            );
        }

        return membership;
    }

    private void validateDateTime(
            LocalDateTime start,
            LocalDateTime end
    ) {

        if (!end.isAfter(start)) {

            throw new InvalidAvailabilityException(
                    "O horário final deve ser posterior ao horário inicial"
            );
        }

        if (start.isBefore(LocalDateTime.now())) {

            throw new InvalidAvailabilityException(
                    "Não é possível criar uma consulta no passado"
            );
        }

        /*
         * Nosso Availability atual trabalha
         * dentro de um único dia.
         */
        if (
                !start.toLocalDate()
                        .equals(
                                end.toLocalDate()
                        )
        ) {

            throw new InvalidAvailabilityException(
                    "A consulta deve começar e terminar no mesmo dia"
            );
        }
    }

    private Psychoanalyst findPsychoanalyst(
            Long id
    ) {

        return psychoanalystRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Psicanalista não encontrado"
                        )
                );
    }

    private Patient findPatient(
            Long id
    ) {

        return patientRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Paciente não encontrado"
                        )
                );
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
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Consulta não encontrada"
                        )
                );
    }

    private AppointmentResponseDTO toResponseDTO(
            Appointment appointment
    ) {

        ClinicMembership membership =
                appointment.getClinicMembership();

        return new AppointmentResponseDTO(

                appointment.getId(),

                appointment
                        .getPatient()
                        .getId(),

                appointment
                        .getPatient()
                        .getUser()
                        .getName(),

                appointment
                        .getPsychoanalyst()
                        .getId(),

                appointment
                        .getPsychoanalyst()
                        .getUser()
                        .getName(),

                membership != null
                        ? membership.getId()
                        : null,

                membership != null
                        ? membership.getClinic().getId()
                        : null,

                membership != null
                        ? membership.getClinic().getName()
                        : null,

                appointment.getOriginalAppointment() != null
                        ? appointment
                                .getOriginalAppointment()
                                .getId()
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