package com.psicogest.psicogest.domain.finance;

import org.springframework.stereotype.Component;

import com.psicogest.psicogest.exception.InvalidFinanceTransitionException;
import com.psicogest.psicogest.model.entity.Receivable.ReceivableStatus;

@Component
public class ReceivableStateMachine {

    public void validateTransition(
            ReceivableStatus current,
            ReceivableStatus target
    ) {

        boolean allowed =
                switch (current) {

                    case OPEN ->
                            target == ReceivableStatus.PARTIALLY_PAID
                            ||
                            target == ReceivableStatus.PAID
                            ||
                            target == ReceivableStatus.CANCELLED
                            ||
                            target == ReceivableStatus.CANCELLATION_PENDING;

                    case PARTIALLY_PAID ->
                            target == ReceivableStatus.PAID
                            ||
                            target == ReceivableStatus.CANCELLED
                            ||
                            target == ReceivableStatus.CANCELLATION_PENDING;

                    case PAID ->
                            target == ReceivableStatus.CANCELLED
                            ||
                            target == ReceivableStatus.CANCELLATION_PENDING;

                    case CANCELLATION_PENDING ->
                            target == ReceivableStatus.CANCELLED;

                    case CANCELLED ->
                            false;
                };

        if (!allowed) {

            throw new InvalidFinanceTransitionException(
                    "Transição financeira não permitida: "
                            + current
                            + " -> "
                            + target
            );
        }
    }
}