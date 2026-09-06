package com.psicogest.psicogest.domain.relationship;

import com.psicogest.psicogest.exception.InvalidTherapeuticRelationshipTransitionException;
import com.psicogest.psicogest.model.enums.TherapeuticRelationshipStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class TherapeuticRelationshipStateMachine {

        private static final Map<TherapeuticRelationshipStatus, Set<TherapeuticRelationshipStatus>> TRANSITIONS;

        static {

                Map<TherapeuticRelationshipStatus, Set<TherapeuticRelationshipStatus>> transitions = new EnumMap<>(
                                TherapeuticRelationshipStatus.class);

                transitions.put(
                                TherapeuticRelationshipStatus.ACTIVE,
                                EnumSet.of(
                                                TherapeuticRelationshipStatus.SUSPENDED,
                                                TherapeuticRelationshipStatus.ENDED));

                transitions.put(
                                TherapeuticRelationshipStatus.SUSPENDED,
                                EnumSet.of(
                                                TherapeuticRelationshipStatus.ACTIVE,
                                                TherapeuticRelationshipStatus.ENDED));

                transitions.put(
                                TherapeuticRelationshipStatus.ENDED,
                                EnumSet.noneOf(
                                                TherapeuticRelationshipStatus.class));

                TRANSITIONS = Map.copyOf(transitions);
        }

        public void validateTransition(
                        TherapeuticRelationshipStatus current,
                        TherapeuticRelationshipStatus target) {

                if (current == null || target == null) {

                        throw new InvalidTherapeuticRelationshipTransitionException(
                                        "Status do vínculo terapêutico inválido");
                }

                if (current == target) {

                        throw new InvalidTherapeuticRelationshipTransitionException(
                                        "O vínculo já possui o status " + current);
                }

                Set<TherapeuticRelationshipStatus> allowed = TRANSITIONS.get(current);

                if (allowed == null
                                || !allowed.contains(target)) {

                        throw new InvalidTherapeuticRelationshipTransitionException(
                                        "Não é permitido alterar o vínculo de "
                                                        + current
                                                        + " para "
                                                        + target);
                }
        }
}