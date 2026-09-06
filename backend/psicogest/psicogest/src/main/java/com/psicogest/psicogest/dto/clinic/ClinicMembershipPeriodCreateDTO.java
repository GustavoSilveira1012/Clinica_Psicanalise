package com.psicogest.psicogest.dto.clinic;

import java.time.LocalDateTime;

public record ClinicMembershipPeriodCreateDTO(

        /*
         * Normalmente null = agora.
         *
         * Permite importar histórico.
         */
        LocalDateTime startedAt

) {
}