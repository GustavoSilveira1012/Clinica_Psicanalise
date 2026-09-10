package com.psicogest.psicogest.service;

import com.psicogest.psicogest.domain.finance.MoneyRules;
import com.psicogest.psicogest.model.entity.Payment;
import com.psicogest.psicogest.model.entity.Receivable;
import com.psicogest.psicogest.repository.PaymentAllocationRepository;
import com.psicogest.psicogest.repository.RefundAllocationRepository;
import com.psicogest.psicogest.repository.RefundRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Service para cálculos de saldos financeiros
 * 
 * 15. Refund amounts confirmados
 * 16. Alocações efetivas (considerando refunds)
 * 17. Saldo disponível do payment
 * 18. Allocated amount de receivable (considerando refunds)
 */
@Service
@Transactional(readOnly = true)
public class FinanceBalanceService {

    private final PaymentAllocationRepository allocationRepository;
    private final RefundRepository refundRepository;
    private final RefundAllocationRepository refundAllocationRepository;

    public FinanceBalanceService(
            PaymentAllocationRepository allocationRepository,
            RefundRepository refundRepository,
            RefundAllocationRepository refundAllocationRepository
    ) {
        this.allocationRepository = allocationRepository;
        this.refundRepository = refundRepository;
        this.refundAllocationRepository = refundAllocationRepository;
    }

    /**
     * 15. Valor total reembolsado confirmado
     */
    public BigDecimal confirmedRefundAmount(UUID paymentId) {

        return MoneyRules.normalize(
                refundRepository
                        .sumConfirmedRefunds(paymentId)
        );
    }

    /**
     * 15. Saldo efetivo do payment
     * = amount - confirmed refunds
     */
    public BigDecimal effectivePaymentAmount(Payment payment) {

        return payment
                .getAmount()
                .subtract(
                        confirmedRefundAmount(
                                payment.getId()
                        )
                );
    }

    /**
     * 16. Alocações brutas (sem considerar refunds)
     */
    public BigDecimal grossAllocatedByPayment(UUID paymentId) {

        return MoneyRules.normalize(
                allocationRepository
                        .sumGrossAllocatedByPayment(paymentId)
        );
    }

    /**
     * 16. Alocações reembolsadas confirmadas
     */
    public BigDecimal confirmedReversedByPayment(UUID paymentId) {

        return MoneyRules.normalize(
                refundAllocationRepository
                        .sumConfirmedReversedByPayment(paymentId)
        );
    }

    /**
     * 16. Alocações efetivas
     * = gross allocations - confirmed refund allocations
     */
    public BigDecimal effectiveAllocatedByPayment(UUID paymentId) {

        return MoneyRules.normalize(

                grossAllocatedByPayment(paymentId)
                        .subtract(
                                confirmedReversedByPayment(paymentId)
                        )
        );
    }

    /**
     * 17. Saldo disponível para alocar
     * = effectivePayment - effectiveAllocated
     */
    public BigDecimal availablePaymentAmount(Payment payment) {

        BigDecimal effectivePayment =
                effectivePaymentAmount(payment);

        BigDecimal allocations =
                effectiveAllocatedByPayment(
                        payment.getId()
                );

        return effectivePayment
                .subtract(allocations)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    /**
     * 13. Calcula alocações mínimas necessárias para refund
     * 
     * minimumAllocationReversal = max(refundAmount - unallocated, 0)
     */
    public BigDecimal minimumAllocationReversal(
            Payment payment,
            BigDecimal refundAmount
    ) {

        BigDecimal available =
                availablePaymentAmount(payment);

        BigDecimal needed =
                refundAmount.subtract(available);

        return needed.max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    /**
     * 18. Alocações brutas para receivable
     */
    public BigDecimal grossAllocatedForReceivable(UUID receivableId) {

        return MoneyRules.normalize(
                allocationRepository
                        .sumGrossForReceivable(receivableId)
        );
    }

    /**
     * 18. Alocações reembolsadas para receivable
     */
    public BigDecimal refundedForReceivable(UUID receivableId) {

        return MoneyRules.normalize(
                refundAllocationRepository
                        .sumRefundedForReceivable(receivableId)
        );
    }

    /**
     * 18. Alocações efetivas do receivable
     * (considerando refunds)
     */
    public BigDecimal allocatedAmount(UUID receivableId) {

        return MoneyRules.normalize(

                grossAllocatedForReceivable(receivableId)
                        .subtract(
                                refundedForReceivable(receivableId)
                        )
        );
    }

    /**
     * Saldo pendente do receivable
     */
    public BigDecimal outstandingAmount(Receivable receivable) {

        return receivable
                .getNetAmount()
                .subtract(
                        allocatedAmount(
                                receivable.getId()
                        )
                )
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_EVEN);
    }
}
