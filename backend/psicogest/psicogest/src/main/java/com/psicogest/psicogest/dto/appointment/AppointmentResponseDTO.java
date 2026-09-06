package com.psicogest.psicogest.dto.appointment;

import com.psicogest.psicogest.model.enums.AppointmentStatus;
import com.psicogest.psicogest.model.enums.AppointmentType;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentResponseDTO(

                Long id,

                Long patientId,

                String patientName,

                Long psychoanalystId,

                String psychoanalystName,

                Long clinicMembershipId,

                Long clinicId,

                String clinicName,

                Long originalAppointmentId,

                UUID appointmentSeriesId,

                Integer occurrenceNumber,

                LocalDateTime scheduledStart,

                LocalDateTime scheduledEnd,

                AppointmentStatus status,

                AppointmentType appointmentType,

                String cancellationReason,

                LocalDateTime cancelledAt,

                LocalDateTime confirmedAt,

                LocalDateTime completedAt,

                LocalDateTime noShowAt,

                LocalDateTime rescheduledAt

) {
}