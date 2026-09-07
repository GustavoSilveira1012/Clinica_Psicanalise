package com.psicogest.psicogest.security;

import com.psicogest.psicogest.exception.ClinicalAccessForbiddenException;
import com.psicogest.psicogest.model.entity.Psychoanalyst;
import com.psicogest.psicogest.model.entity.User;
import com.psicogest.psicogest.model.enums.UserRole;
import com.psicogest.psicogest.repository.PsychoanalystRepository;
import com.psicogest.psicogest.security.authorization.ClinicalAccessEvaluator;
import com.psicogest.psicogest.service.TherapeuticRelationshipService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClinicalAccessEvaluatorTest {

    @Mock
    private TherapeuticRelationshipService relationshipService;

    @Mock
    private PsychoanalystRepository psychoanalystRepository;

    @InjectMocks
    private ClinicalAccessEvaluator evaluator;

    @Test
    void shouldDenyAccessIfUserNotPsychoanalyst() {
        User user = new User();
        user.setId(1L);
        user.setRole(UserRole.PATIENT);

        assertThatThrownBy(() -> evaluator.validateClinicalAccess(user, 100L))
                .isInstanceOf(ClinicalAccessForbiddenException.class)
                .hasMessageContaining("O usuário não tem perfil de psicanalista");

        verifyNoInteractions(psychoanalystRepository, relationshipService);
    }

    @Test
    void shouldDenyAccessIfPsychoanalystNotFound() {
        User user = new User();
        user.setId(1L);
        user.setRole(UserRole.PSYCHOANALYST);

        when(psychoanalystRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> evaluator.validateClinicalAccess(user, 100L))
                .isInstanceOf(ClinicalAccessForbiddenException.class)
                .hasMessageContaining("Psicanalista não encontrado");

        verifyNoInteractions(relationshipService);
    }

    @Test
    void shouldDenyAccessIfNoActiveRelationship() {
        User user = new User();
        user.setId(1L);
        user.setRole(UserRole.PSYCHOANALYST);

        Psychoanalyst psychoanalyst = new Psychoanalyst();
        psychoanalyst.setId(10L);

        when(psychoanalystRepository.findByUserId(1L)).thenReturn(Optional.of(psychoanalyst));
        when(relationshipService.hasCurrentRelationship(100L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> evaluator.validateClinicalAccess(user, 100L))
                .isInstanceOf(ClinicalAccessForbiddenException.class)
                .hasMessageContaining("não há vínculo terapêutico ativo com este paciente");
    }

    @Test
    void shouldAllowAccessIfActiveRelationshipExists() {
        User user = new User();
        user.setId(1L);
        user.setRole(UserRole.PSYCHOANALYST);

        Psychoanalyst psychoanalyst = new Psychoanalyst();
        psychoanalyst.setId(10L);

        when(psychoanalystRepository.findByUserId(1L)).thenReturn(Optional.of(psychoanalyst));
        when(relationshipService.hasCurrentRelationship(100L, 10L)).thenReturn(true);

        // Should not throw any exception
        evaluator.validateClinicalAccess(user, 100L);

        verify(relationshipService).hasCurrentRelationship(100L, 10L);
    }
}
