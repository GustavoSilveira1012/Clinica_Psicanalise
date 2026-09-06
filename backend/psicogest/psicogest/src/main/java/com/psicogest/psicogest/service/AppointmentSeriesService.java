package com.psicogest.psicogest.service;

import com.psicogest.psicogest.dto.appointment.*;
import com.psicogest.psicogest.exception.*;
import com.psicogest.psicogest.model.entity.*;
import com.psicogest.psicogest.model.enums.*;
import com.psicogest.psicogest.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AppointmentSeriesService {

    private final AppointmentSeriesRepository seriesRepository;

    private final AppointmentRepository appointmentRepository;

    private final PatientRepository patientRepository;

    private final PsychoanalystRepository psychoanalystRepository;

    private final ClinicMembershipRepository membershipRepository;

    private final ScheduleAvailabilityService scheduleAvailabilityService;

    public AppointmentSeriesService(
            AppointmentSeriesRepository seriesRepository,
            AppointmentRepository appointmentRepository,
            PatientRepository patientRepository,
            PsychoanalystRepository psychoanalystRepository,
            ClinicMembershipRepository membershipRepository,
            ScheduleAvailabilityService scheduleAvailabilityService
    ) {

        this.seriesRepository =
                seriesRepository;

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
    public AppointmentSeriesResponseDTO create(
            Long psychoanalystId,
            AppointmentSeriesCreateDTO dto
    ) {

        validateSeriesDefinition(dto);

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

        int interval =
                dto.recurrenceInterval() != null
                        ? dto.recurrenceInterval()
                        : 1;

        DayOfWeek dayOfWeek =
                dto.startsOn().getDayOfWeek();

        List<LocalDate> dates =
                generateDates(
                        dto.startsOn(),
                        dto.endsOn(),
                        dto.totalOccurrences(),
                        interval
                );

        LocalTime endTime =
                dto.startTime()
                        .plusMinutes(
                                dto.durationMinutes()
                        );

        /*
         * Primeiro validamos absolutamente tudo.
         */
        for (LocalDate date : dates) {

            validateSchedule(
                    psychoanalystId,
                    date,
                    dto.startTime(),
                    endTime
            );

            validateAppointmentConflict(
                    psychoanalystId,
                    date,
                    dto.startTime(),
                    endTime
            );
        }

        UUID seriesId =
                UUID.randomUUID();

        AppointmentSeries series =
                AppointmentSeries.builder()

                        .id(seriesId)

                        .patient(patient)

                        .psychoanalyst(
                                psychoanalyst
                        )

                        .clinicMembership(
                                membership
                        )

                        .frequency(
                                dto.frequency()
                        )

                        .recurrenceInterval(
                                interval
                        )

                        .dayOfWeek(
                                dayOfWeek
                        )

                        .startTime(
                                dto.startTime()
                        )

                        .durationMinutes(
                                dto.durationMinutes()
                        )

                        .startsOn(
                                dto.startsOn()
                        )

                        .endsOn(
                                dates
                                        .get(
                                                dates.size() - 1
                                        )
                        )

                        .totalOccurrences(
                                dates.size()
                        )

                        .status(
                                AppointmentSeriesStatus.ACTIVE
                        )

                        .build();

        AppointmentSeries savedSeries =
                seriesRepository.save(series);

        List<Appointment> appointments =
                new ArrayList<>();

        int occurrence = 1;

        for (LocalDate date : dates) {

            LocalDateTime start =
                    LocalDateTime.of(
                            date,
                            dto.startTime()
                    );

            LocalDateTime end =
                    start.plusMinutes(
                            dto.durationMinutes()
                    );

            Appointment appointment =
                    Appointment.builder()

                            .patient(patient)

                            .psychoanalyst(
                                    psychoanalyst
                            )

                            .clinicMembership(
                                    membership
                            )

                            .appointmentSeries(
                                    savedSeries
                            )

                            .occurrenceNumber(
                                    occurrence++
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

            appointments.add(
                    appointment
            );
        }

        /*
         * saveAllAndFlush força o banco
         * a aplicar a exclusion constraint.
         */
        appointmentRepository
                .saveAllAndFlush(
                        appointments
                );

        return toResponseDTO(
                savedSeries
        );
    }

    private void validateSeriesDefinition(AppointmentSeriesCreateDTO dto) {
        if (dto == null) {
            throw new InvalidAppointmentSeriesException(
                    "A definição da série de consultas é obrigatória."
            );
        }

        if (dto.patientId() == null
                || dto.frequency() == null
                || dto.startsOn() == null
                || dto.startTime() == null
                || dto.durationMinutes() == null
                || dto.appointmentType() == null) {
            throw new InvalidAppointmentSeriesException(
                    "Os campos obrigatórios da série de consultas devem ser informados."
            );
        }

        boolean hasEndDate = dto.endsOn() != null;
        boolean hasOccurrenceCount = dto.totalOccurrences() != null;

        if (hasEndDate == hasOccurrenceCount) {
            throw new InvalidAppointmentSeriesException(
                                        "Informe totalOccurrences ou endsOn, mas não ambos"
            );
        }

                if (dto.startsOn().isBefore(LocalDate.now())) {
                        throw new InvalidAppointmentSeriesException(
                                        "A série não pode começar no passado"
                        );
                }

        if (hasEndDate && dto.endsOn().isBefore(dto.startsOn())) {
            throw new InvalidAppointmentSeriesException(
                                        "A data final não pode ser anterior à data inicial"
            );
        }

                if (dto.frequency() != RecurrenceFrequency.WEEKLY) {
                        throw new InvalidAppointmentSeriesException(
                                        "Apenas recorrência semanal está disponível atualmente"
                        );
                }

        int interval = dto.recurrenceInterval() != null
                ? dto.recurrenceInterval()
                : 1;

        if (interval < 1 || interval > 12) {
            throw new InvalidAppointmentSeriesException(
                    "O intervalo de recorrência deve estar entre 1 e 12."
            );
        }

        if (dto.durationMinutes() < 10 || dto.durationMinutes() > 480) {
            throw new InvalidAppointmentSeriesException(
                    "A duração deve estar entre 10 e 480 minutos."
            );
        }

        if (hasOccurrenceCount
                && (dto.totalOccurrences() < 2 || dto.totalOccurrences() > 104)) {
            throw new InvalidAppointmentSeriesException(
                    "O número de ocorrências deve estar entre 2 e 104."
            );
        }
    }

        private List<LocalDate> generateDates(
                        LocalDate startsOn,
                        LocalDate endsOn,
                        Integer totalOccurrences,
                        int interval
        ) {
                List<LocalDate> dates = new ArrayList<>();
                LocalDate date = startsOn;

                if (totalOccurrences != null) {
                        for (int occurrence = 0; occurrence < totalOccurrences; occurrence++) {
                                dates.add(date);
                                date = date.plusWeeks(interval);
                        }
                        return dates;
                }

                while (!date.isAfter(endsOn)) {
                        dates.add(date);
                        date = date.plusWeeks(interval);
                }

                if (dates.size() < 2) {
                        throw new InvalidAppointmentSeriesException(
                                        "A série deve possuir pelo menos duas consultas"
                        );
                }

                return dates;
        }

        private void validateSchedule(
                        Long psychoanalystId,
                        LocalDate date,
                        LocalTime startTime,
                        LocalTime endTime
        ) {
                if (!scheduleAvailabilityService.isWithinSchedule(
                                psychoanalystId,
                                date,
                                startTime,
                                endTime
                )) {
                        throw new InvalidAvailabilityException(
                                        "A série possui uma ocorrência fora da disponibilidade em "
                                                        + date
                        );
                }
        }

        private void validateAppointmentConflict(
                        Long psychoanalystId,
                        LocalDate date,
                        LocalTime startTime,
                        LocalTime endTime
        ) {
                LocalDateTime start = LocalDateTime.of(date, startTime);
                LocalDateTime end = LocalDateTime.of(date, endTime);

                boolean conflict = !appointmentRepository.findConflicts(
                                psychoanalystId,
                                start,
                                end,
                                java.util.EnumSet.of(
                                                AppointmentStatus.SCHEDULED,
                                                AppointmentStatus.CONFIRMED
                                ),
                                null
                ).isEmpty();

                if (conflict) {
                        throw new ScheduleConflictException(
                                        "Existe conflito de agenda na data "
                                                        + date
                        );
                }
        }

        private AppointmentSeriesResponseDTO toResponseDTO(
                        AppointmentSeries series
        ) {
                ClinicMembership membership = series.getClinicMembership();

                return new AppointmentSeriesResponseDTO(
                                series.getId(),
                                series.getPatient().getId(),
                                series.getPatient().getUser().getName(),
                                series.getPsychoanalyst().getId(),
                                series.getPsychoanalyst().getUser().getName(),
                                membership != null ? membership.getId() : null,
                                series.getFrequency(),
                                series.getRecurrenceInterval(),
                                series.getDayOfWeek(),
                                series.getStartTime(),
                                series.getDurationMinutes(),
                                series.getStartsOn(),
                                series.getEndsOn(),
                                series.getTotalOccurrences(),
                                series.getStatus()
                );
        }

                    private Psychoanalyst findPsychoanalyst(Long id) {
                        return psychoanalystRepository
                                .findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                        "Psicanalista não encontrado"
                                ));
                    }

                    private Patient findPatient(Long id) {
                        return patientRepository
                                .findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                        "Paciente não encontrado"
                                ));
                    }

                    private ClinicMembership resolveMembership(
                            Long psychoanalystId,
                            Long membershipId
                    ) {
                        if (membershipId == null) {
                            return null;
                        }

                        ClinicMembership membership = membershipRepository
                                .findByIdAndPsychoanalystId(
                                        membershipId,
                                        psychoanalystId
                                )
                                .orElseThrow(() -> new ResourceNotFoundException(
                                        "Vínculo com clínica não encontrado"
                                ));

                        if (membership.getStatus() != MembershipStatus.ACTIVE) {
                            throw new ScheduleConflictException(
                                    "O vínculo com a clínica não está ativo"
                            );
                        }

                        return membership;
                    }
}