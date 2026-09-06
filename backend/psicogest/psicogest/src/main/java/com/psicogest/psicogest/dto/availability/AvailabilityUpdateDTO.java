package com.psicogest.psicogest.dto.availability;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record AvailabilityUpdateDTO(

                @NotNull(message = "Dia da semana é obrigatório") DayOfWeek dayOfWeek,

                @NotNull(message = "Horário inicial é obrigatório") LocalTime startTime,

                @NotNull(message = "Horário final é obrigatório") LocalTime endTime,

                @NotNull(message = "Status ativo é obrigatório") Boolean active) {
}