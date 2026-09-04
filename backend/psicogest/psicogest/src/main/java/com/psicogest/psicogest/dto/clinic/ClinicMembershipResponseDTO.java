package com.psicogest.psicogest.dto.clinic;

import com.psicogest.psicogest.model.enums.MembershipStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ClinicMembershipResponseDTO(
        Long id,

        Long clinicId,

        String clinicName,

        Long psychoanalystId,

        String psychoanalystName,

        MembershipStatus status,

        LocalDateTime joinedAt
) {    
}