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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import com.psicogest.psicogest.exception.TenantContextRequiredException;
import com.psicogest.psicogest.security.tenant.TenantContext;
import com.psicogest.psicogest.security.tenant.TenantContextHolder;

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
                    .findByUserIdAndOrganizationId(userId, organizationId())
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
                    .filter(patient -> organizationId().equals(patient.getOrganizationId()))
                    .map(this::toResponse)
                    .toList();
        }

        if (context.hasRole(authentication, "CLINIC_ADMIN")) {
            return patientRepository.findByOrganizationIdAndActiveTrue(organizationId())
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }

        throw new AccessDeniedException("Acesso negado");
    }

    public Page<PatientResponseDTO> search(
            Authentication authentication,
            String query,
            Boolean active,
            Pageable pageable
    ) {
        Long userId = context.userId(authentication)
                .orElseThrow(() -> new AccessDeniedException("Acesso negado"));
        String normalizedQuery = query == null ? null : query.trim();
        var organizationId = organizationId();

        if (context.hasRole(authentication, "CLINIC_ADMIN")) {
            return patientRepository.searchByOrganization(
                            organizationId, normalizedQuery, active, pageable)
                    .map(this::toResponse);
        }

        List<Patient> visible;
        if (context.hasRole(authentication, "PATIENT")) {
            visible = patientRepository.findByUserIdAndOrganizationId(userId, organizationId)
                    .filter(patient -> matches(patient, normalizedQuery, active))
                    .map(List::of)
                    .orElseGet(List::of);
        } else if (context.hasRole(authentication, "PSYCHOANALYST")) {
            Psychoanalyst psychoanalyst = psychoanalystRepository.findByUserId(userId).orElseThrow();
            visible = relationshipRepository.searchAccessiblePatients(
                    psychoanalyst.getId(), organizationId,
                    EnumSet.of(TherapeuticRelationshipStatus.ACTIVE, TherapeuticRelationshipStatus.SUSPENDED),
                    normalizedQuery, active);
        } else {
            throw new AccessDeniedException("Acesso negado");
        }

        int start = Math.min((int) pageable.getOffset(), visible.size());
        int end = Math.min(start + pageable.getPageSize(), visible.size());
        return new PageImpl<>(visible.subList(start, end).stream().map(this::toResponse).toList(), pageable, visible.size());
    }

    private boolean matches(Patient patient, String query, Boolean active) {
        if (active != null && !active.equals(patient.getActive())) return false;
        if (query == null || query.isBlank()) return true;
        String normalized = query.toLowerCase(java.util.Locale.ROOT);
        return patient.getUser().getName().toLowerCase(java.util.Locale.ROOT).contains(normalized)
                || patient.getUser().getEmail().toLowerCase(java.util.Locale.ROOT).contains(normalized);
    }

    private java.util.UUID organizationId() {
        TenantContext tenant = TenantContextHolder.get();
        if (tenant == null || tenant.organizationId() == null) {
            throw new TenantContextRequiredException("Contexto de organização obrigatório");
        }
        return tenant.organizationId();
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
