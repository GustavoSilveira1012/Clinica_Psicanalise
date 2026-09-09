package com.psicogest.psicogest.domain.medicalrecord;

import com.psicogest.psicogest.exception.InvalidMedicalRecordTransitionException;
import com.psicogest.psicogest.model.enums.MedicalRecordStatus;
import org.springframework.stereotype.Component;

@Component
public class MedicalRecordStateMachine {

    public void validateTransition(
            MedicalRecordStatus current,
            MedicalRecordStatus target
    ) {

        if (current == null || target == null) {

            throw new InvalidMedicalRecordTransitionException(
                    "Status do prontuário inválido"
            );
        }

        if (current == target) {

            throw new InvalidMedicalRecordTransitionException(
                    "O prontuário já está em " + current
            );
        }

        if (
                current == MedicalRecordStatus.DRAFT
                && target == MedicalRecordStatus.FINALIZED
        ) {
            return;
        }

        throw new InvalidMedicalRecordTransitionException(
                "Transição de prontuário não permitida: "
                        + current
                        + " -> "
                        + target
        );
    }
}