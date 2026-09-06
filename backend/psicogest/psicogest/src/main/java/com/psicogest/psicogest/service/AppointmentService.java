package com.psicogest.psicogest.service;

import com.psicogest.psicogest.dto.appointment.*;
import com.psicogest.psicogest.exception.InvalidAvailabilityException;
import com.psicogest.psicogest.exception.ResourceNotFoundException;
import com.psicogest.psicogest.exception.ScheduleConflictException;
import com.psicogest.psicogest.model.entity.*;
import com.psicogest.psicogest.model.enums.AppointmentStatus;
import com.psicogest.psicogest.model.enums.MembershipStatus;
import com.psicogest.psicogest.repository.*;
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

    private final PatientRepository patientRepository;

    private final PsychoanalystRepository psychoanalystRepository;

    private final ClinicMembershipRepository membershipRepository;

    private final ScheduleAvailabilityService scheduleAvailabilityService;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            PatientRepository patientRepository,
            PsychoanalystRepository psychoanalystRepository,
            ClinicMembershipRepository membershipRepository,
            ScheduleAvailabilityService scheduleAvailabilityService
    ) {

        this.appointmentRepository =
                appointmentRepository;

        this.patientRepository =
                patientRepository;

        this.psychoanalystRepository =
                psychoanalystRepository;

        this.membershipRepository =
                membershipRepository;

        this.scheduleAvailabilityService =
                scheduleAvailabilityService;
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
                        dto.clinicMembershipId()
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
                appointmentRepository.save(
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

        if (
                appointment.getStatus()
                        == AppointmentStatus.CANCELLED
        ) {

            throw new ScheduleConflictException(
                    "A consulta já está cancelada"
            );
        }

        if (
                appointment.getStatus()
                        == AppointmentStatus.COMPLETED
        ) {

            throw new ScheduleConflictException(
                    "Uma consulta concluída não pode ser cancelada"
            );
        }

        if (
                appointment.getStatus()
                        == AppointmentStatus.RESCHEDULED
        ) {

            throw new ScheduleConflictException(
                    "Esta consulta já foi reagendada"
            );
        }

        appointment.setStatus(
                AppointmentStatus.CANCELLED
        );

        appointment.setCancelledAt(
                LocalDateTime.now()
        );

        appointment.setCancellationReason(
                dto.reason().trim()
        );

        return toResponseDTO(
                appointmentRepository.save(
                        appointment
                )
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

        validateCanReschedule(
                original
        );

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
        original.setStatus(
                AppointmentStatus.RESCHEDULED
        );

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
                        .recurringGroupId(
                                original.getRecurringGroupId()
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

        return toResponseDTO(
                appointmentRepository.save(
                        newAppointment
                )
        );
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
                        dto.clinicMembershipId()
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

        UUID recurringGroupId =
                UUID.randomUUID();

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
                                    .recurringGroupId(
                                            recurringGroupId
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

        return appointmentRepository
                .saveAll(appointments)
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

    private ClinicMembership resolveMembership(
            Long psychoanalystId,
            Long membershipId
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

        if (
                membership.getStatus()
                        != MembershipStatus.ACTIVE
        ) {

            throw new ScheduleConflictException(
                    "O vínculo do psicanalista com esta clínica não está ativo"
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

    private void validateCanReschedule(
            Appointment appointment
    ) {

        if (
                appointment.getStatus()
                        == AppointmentStatus.CANCELLED
        ) {

            throw new ScheduleConflictException(
                    "Consulta cancelada não pode ser reagendada"
            );
        }

        if (
                appointment.getStatus()
                        == AppointmentStatus.COMPLETED
        ) {

            throw new ScheduleConflictException(
                    "Consulta concluída não pode ser reagendada"
            );
        }

        if (
                appointment.getStatus()
                        == AppointmentStatus.RESCHEDULED
        ) {

            throw new ScheduleConflictException(
                    "A consulta já foi reagendada"
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

                appointment.getRecurringGroupId(),

                appointment.getScheduledStart(),

                appointment.getScheduledEnd(),

                appointment.getStatus(),

                appointment.getAppointmentType(),

                appointment.getCancellationReason(),

                appointment.getCancelledAt()
        );
    }
}