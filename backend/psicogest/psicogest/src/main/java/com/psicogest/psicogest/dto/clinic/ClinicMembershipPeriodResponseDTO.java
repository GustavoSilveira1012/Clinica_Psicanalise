package com.psicogest.psicogest.dto.clinic;

import com.psicogest.psicogest.model.enums.ClinicMembershipPeriodStatus;

import java.time.LocalDateTime;

public record ClinicMembershipPeriodResponseDTO (
        Long id,

        Long clinicMembershipId,

        Long clinicId,

        String clinicName,

        Long psychoanalystId,

        String psychoanalystName,

        ClinicMembershipPeriodStatus status,

        LocalDateTime startedAt,

        LocalDateTime endedAt,

        String endReason
) {
    
}
