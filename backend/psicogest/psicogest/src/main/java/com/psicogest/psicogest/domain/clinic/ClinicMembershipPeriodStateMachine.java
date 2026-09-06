package com.psicogest.psicogest.domain.clinic;

import com.psicogest.psicogest.exception.InvalidClinicMembershipPeriodTransitionException;
import com.psicogest.psicogest.model.enums.ClinicMembershipPeriodStatus;
import org.springframework.stereotype.Component;

@Component
public class ClinicMembershipPeriodStateMachine {

    public void validateTransition(
            ClinicMembershipPeriodStatus current,
            ClinicMembershipPeriodStatus target) {

        if (current == null || target == null) {

            throw new InvalidClinicMembershipPeriodTransitionException(
                    "Status do período de vínculo inválido");
        }

        if (current == target) {

            throw new InvalidClinicMembershipPeriodTransitionException(
                    "O período já possui o status " + current);
        }

        if (current == ClinicMembershipPeriodStatus.ACTIVE
                && target == ClinicMembershipPeriodStatus.ENDED) {
            return;
        }

        throw new InvalidClinicMembershipPeriodTransitionException(
                "Não é permitido alterar o período de "
                        + current
                        + " para "
                        + target);
    }
}