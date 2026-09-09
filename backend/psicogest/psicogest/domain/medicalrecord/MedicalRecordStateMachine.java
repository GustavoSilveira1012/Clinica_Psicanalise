package com.psicogest.psicogest.domain.medicalrecord;

import com.psicogest.psicogest.model.enums.MedicalRecordStatus;
import org.springframework.stereotype.Component;

@Component
public class MedicalRecordStateMachine {

    public boolean canTransition(
            MedicalRecordStatus from,
            MedicalRecordStatus to
    ) {

        return switch ( from ) {

            case DRAFT ->
                    to == MedicalRecordStatus.FINALIZED;

            case FINALIZED ->
                    false;
        };
    }

    public void validateTransition(
            MedicalRecordStatus from,
            MedicalRecordStatus to
    ) {

        if ( !canTransition( from, to ) ) {

            throw new IllegalStateException(
                    "Transição inválida de "
                    + from
                    + " para "
                    + to
            );
        }
    }
}
