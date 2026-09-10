package com.psicogest.psicogest.service;

import com.psicogest.psicogest.model.entity.Receivable;
import com.psicogest.psicogest.repository.PaymentAllocationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * 28. Service para cálculos de saldo financeiro
 * 
 * Encapsula lógica de saldo, evitando SQL financeiro
 * espalhado pelo sistema
 * 
 * Futuro: refunds serão integrados em sumEffectiveAllocation()
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class FinanceBalanceService {

    private final PaymentAllocationRepository allocationRepository;

    public FinanceBalanceService(
            PaymentAllocationRepository allocationRepository
    ) {
        this.allocationRepository = allocationRepository;
    }

    /**
     * Calcula valor total alocado (pago) para uma conta
     * 
     * @param receivableId ID da conta
     * @return valor alocado com 2 casas decimais
     */
    public BigDecimal allocatedAmount(
            UUID receivableId
    ) {

        BigDecimal amount =
                allocationRepository
                        .sumEffectiveAllocation(
                                receivableId
                        );

        return amount != null
                ? amount
                    .setScale(
                            2,
                            RoundingMode.HALF_EVEN
                    )
                : BigDecimal.ZERO
                    .setScale(2);
    }

    /**
     * Calcula saldo a receber (valor pendente)
     * 
     * outstandingAmount =
     *   netAmount - allocatedAmount
     *   (nunca negativo)
     * 
     * @param receivable conta a receber
     * @return valor pendente com 2 casas decimais
     */
    public BigDecimal outstandingAmount(
            Receivable receivable
    ) {

        return receivable
                .getNetAmount()

                .subtract(
                        allocatedAmount(
                                receivable.getId()
                        )
                )

                .max(
                        BigDecimal.ZERO
                );
    }
}
