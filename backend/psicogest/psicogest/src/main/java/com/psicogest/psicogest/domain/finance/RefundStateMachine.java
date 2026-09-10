package com.psicogest.psicogest.domain.finance;

import com.psicogest.psicogest.exception.InvalidFinanceTransitionException;
import com.psicogest.psicogest.model.entity.Refund.RefundStatus;
import org.springframework.stereotype.Component;

/**
 * 6. State machine para transições de refund
 * 
 * Estados:
 * PENDING → CONFIRMED (sucesso)
 * PENDING → FAILED (erro)
 * PENDING → CANCELLED (cancelado)
 * 
 * Nenhuma outra transição é permitida
 */
@Component
public class RefundStateMachine {

    /**
     * Valida transição de estado
     * 
     * @param current estado atual
     * @param target estado alvo
     * @throws InvalidFinanceTransitionException se transição inválida
     */
    public void validateTransition(
            RefundStatus current,
            RefundStatus target
    ) {

        boolean allowed =
                current == RefundStatus.PENDING
                &&
                (
                        target == RefundStatus.CONFIRMED
                        ||
                        target == RefundStatus.FAILED
                        ||
                        target == RefundStatus.CANCELLED
                );

        if (!allowed) {

            throw new InvalidFinanceTransitionException(
                    "Transição de refund não permitida: "
                            + current
                            + " -> "
                            + target
            );
        }
    }
}
