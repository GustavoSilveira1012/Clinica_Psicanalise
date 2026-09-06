package com.psicogest.psicogest.dto.appointment;

import com.psicogest.psicogest.model.enums.AppointmentType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AppointmentCreateDTO(

                @NotNull(message = "Paciente é obrigatório") Long patientId,

                /*
                 * Opcional porque um profissional
                 * pode atender fora de uma clínica.
                 */
                Long clinicMembershipId,

                @NotNull(message = "Data inicial é obrigatória") LocalDateTime scheduledStart,

                @NotNull(message = "Data final é obrigatória") LocalDateTime scheduledEnd,

                @NotNull(message = "Tipo do atendimento é obrigatório") AppointmentType appointmentType

) {
}