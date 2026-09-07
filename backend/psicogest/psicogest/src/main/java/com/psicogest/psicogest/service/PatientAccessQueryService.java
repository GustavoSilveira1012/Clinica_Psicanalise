package com.psicogest.psicogest.service;

import com.psicogest.psicogest.dto.patient.PatientResponseDTO;
import com.psicogest.psicogest.model.entity.Patient;
import com.psicogest.psicogest.model.entity.Psychoanalyst;
import com.psicogest.psicogest.model.enums.TherapeuticRelationshipStatus;
import com.psicogest.psicogest.repository.PatientRepository;
import com.psicogest.psicogest.repository.PsychoanalystRepository;
import com.psicogest.psicogest.repository.TherapeuticRelationshipRepository;
import com.psicogest.psicogest.security.authorization.AuthenticatedUserContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PatientAccessQueryService {

    private final AuthenticatedUserContext context;
    private final PatientRepository patientRepository;
    private final PsychoanalystRepository psychoanalystRepository;
    private final TherapeuticRelationshipRepository relationshipRepository;

    public PatientAccessQueryService(
            AuthenticatedUserContext context,
            PatientRepository patientRepository,
            PsychoanalystRepository psychoanalystRepository,
            TherapeuticRelationshipRepository relationshipRepository
    ) {
        this.context = context;
        this.patientRepository = patientRepository;
        this.psychoanalystRepository = psychoanalystRepository;
        this.relationshipRepository = relationshipRepository;
    }

    public List<PatientResponseDTO> findAccessible(Authentication authentication) {
        Long userId = context
                .userId(authentication)
                .orElseThrow(() -> new AccessDeniedException("Acesso negado"));

        if (context.hasRole(authentication, "PATIENT")) {
            return patientRepository
                    .findByUserId(userId)
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }

        if (context.hasRole(authentication, "PSYCHOANALYST")) {
            Psychoanalyst psychoanalyst = psychoanalystRepository
                    .findByUserId(userId)
                    .orElseThrow();

            return relationshipRepository
                    .findAccessiblePatients(
                            psychoanalyst.getId(),
                            EnumSet.of(
                                    TherapeuticRelationshipStatus.ACTIVE,
                                    TherapeuticRelationshipStatus.SUSPENDED
                            )
                    )
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }

        throw new AccessDeniedException("Acesso negado");
    }

    private PatientResponseDTO toResponse(Patient patient) {
        return new PatientResponseDTO(
                patient.getId(),
                patient.getUser().getName(),
                patient.getUser().getEmail(),
                patient.getPhone(),
                patient.getBirthDate(),
                patient.getActive()
        );
    }
}
