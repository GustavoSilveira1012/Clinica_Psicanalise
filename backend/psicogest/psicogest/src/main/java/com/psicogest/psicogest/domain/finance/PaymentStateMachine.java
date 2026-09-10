package com.psicogest.psicogest.domain.finance;

import org.springframework.stereotype.Component;

import com.psicogest.psicogest.exception.InvalidFinanceTransitionException;
import com.psicogest.psicogest.model.entity.Payment.PaymentStatus;

@Component
public class PaymentStateMachine {

    public void validateTransition(
            PaymentStatus current,
            PaymentStatus target
    ) {

        boolean allowed =
                switch (current) {

                    case PENDING ->
                            target == PaymentStatus.CONFIRMED
                            ||
                            target == PaymentStatus.FAILED
                            ||
                            target == PaymentStatus.CANCELLED;

                    case CONFIRMED ->
                            target == PaymentStatus.PARTIALLY_REFUNDED
                            ||
                            target == PaymentStatus.REFUNDED;

                    case PARTIALLY_REFUNDED ->
                            target == PaymentStatus.REFUNDED;

                    case FAILED,
                         CANCELLED,
                         REFUNDED ->
                            false;
                };

        if (!allowed) {

            throw new InvalidFinanceTransitionException(
                    "Transição de pagamento não permitida: "
                            + current
                            + " -> "
                            + target
            );
        }
    }
}