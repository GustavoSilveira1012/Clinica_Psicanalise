package com.psicogest.psicogest.security.authorization;

import com.psicogest.psicogest.exception.ClinicalAccessForbiddenException;
import com.psicogest.psicogest.model.entity.User;
import com.psicogest.psicogest.model.enums.UserRole;
import com.psicogest.psicogest.service.TherapeuticRelationshipService;
import com.psicogest.psicogest.repository.PsychoanalystRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ClinicalAccessEvaluator {

    private final TherapeuticRelationshipService relationshipService;
    private final PsychoanalystRepository psychoanalystRepository;

    public ClinicalAccessEvaluator(
            TherapeuticRelationshipService relationshipService,
            PsychoanalystRepository psychoanalystRepository) {
        this.relationshipService = relationshipService;
        this.psychoanalystRepository = psychoanalystRepository;
    }

    @Transactional(readOnly = true)
    public void validateClinicalAccess(User user, Long patientId) {
        if (user.getRole() != UserRole.PSYCHOANALYST) {
            throw new ClinicalAccessForbiddenException("O usuário não tem perfil de psicanalista");
        }

        Long psychoanalystId = psychoanalystRepository.findByUserId(user.getId())
                .map(p -> p.getId())
                .orElseThrow(() -> new ClinicalAccessForbiddenException("Psicanalista não encontrado para o usuário"));

        if (!relationshipService.hasCurrentRelationship(patientId, psychoanalystId)) {
            throw new ClinicalAccessForbiddenException("Acesso negado: não há vínculo terapêutico ativo com este paciente");
        }
    }
}
