package com.psicogest.psicogest.service;

import com.psicogest.psicogest.domain.finance.MoneyRules;
import com.psicogest.psicogest.model.entity.Payment;
import com.psicogest.psicogest.model.entity.PaymentAllocation;
import com.psicogest.psicogest.model.entity.Receivable;
import com.psicogest.psicogest.repository.CreditEntryRepository;
import com.psicogest.psicogest.repository.PaymentAllocationRepository;
import com.psicogest.psicogest.repository.ReceivableAdjustmentRepository;
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
    private final CreditEntryRepository creditEntryRepository;
    private final ReceivableAdjustmentRepository receivableAdjustmentRepository;

    public FinanceBalanceService(
            PaymentAllocationRepository allocationRepository,
            RefundRepository refundRepository,
            RefundAllocationRepository refundAllocationRepository,
            CreditEntryRepository creditEntryRepository,
            ReceivableAdjustmentRepository receivableAdjustmentRepository
    ) {
        this.allocationRepository = allocationRepository;
        this.refundRepository = refundRepository;
        this.refundAllocationRepository = refundAllocationRepository;
        this.creditEntryRepository = creditEntryRepository;
        this.receivableAdjustmentRepository = receivableAdjustmentRepository;
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
     * 
     * outstandingAmount = effectiveAmount - paid
     * (não pode ser negativo; se negativo, é overpaid)
     */
    public BigDecimal outstandingAmount(Receivable receivable) {

        BigDecimal effectiveAmount =
                effectiveReceivableAmount(receivable);

        BigDecimal paid =
                allocatedAmount(receivable.getId());

        return effectiveAmount
                .subtract(paid)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    /**
     * Valor pago em excesso (overpaid)
     * 
     * overpaidAmount = paid - effectiveAmount
     * (não pode ser negativo; se negativo, é outstanding)
     * 
     * Isso será exatamente o que precisaremos devolver/transformar em crédito
     */
    public BigDecimal overpaidAmount(Receivable receivable) {

        BigDecimal effectiveAmount =
                effectiveReceivableAmount(receivable);

        BigDecimal paid =
                allocatedAmount(receivable.getId());

        return paid
                .subtract(effectiveAmount)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    /**
     * 15. Soma créditos originados de cancelamento de cobrança
     */
    public BigDecimal sumCancellationCredits(UUID receivableId) {

        return MoneyRules.normalize(
                creditEntryRepository
                        .sumCancellationCredits(receivableId)
        );
    }

    /**
     * 15. Soma créditos aplicados em uma cobrança
     */
    public BigDecimal sumAppliedCredits(UUID receivableId) {

        return MoneyRules.normalize(
                creditEntryRepository
                        .sumAppliedCredits(receivableId)
        );
    }

    /**
     * 15. Evoluído: allocatedAmount considerando créditos
     * 
     * effectivePaid =
     *   grossPaymentAllocations
     *   - refunds
     *   - allocationsConvertedToCredit
     *   + creditsAppliedToReceivable
     */
    public BigDecimal allocatedAmountWithCredits(UUID receivableId) {

        BigDecimal gross =
                grossAllocatedForReceivable(receivableId);

        BigDecimal refunded =
                refundedForReceivable(receivableId);

        BigDecimal movedToCredit =
                sumCancellationCredits(receivableId);

        BigDecimal appliedCredit =
                sumAppliedCredits(receivableId);

        return MoneyRules.normalize(

                gross

                        .subtract(refunded)

                        .subtract(movedToCredit)

                        .add(appliedCredit)
        );
    }

    /**
     * 20. Valor efetivamente aplicado de uma alocação
     * (considerando refunds e créditos gerados)
     */
    public BigDecimal effectiveAllocationAmount(
            PaymentAllocation allocation
    ) {

        BigDecimal refunded =
                refundAllocationRepository
                        .sumConfirmedRefundedAmount(
                                allocation.getId()
                        );

        BigDecimal credited =
                creditEntryRepository
                        .sumCreditCreatedFromAllocation(
                                allocation.getId()
                        );

        return MoneyRules.normalize(

                allocation.getAmount()

                        .subtract(refunded)

                        .subtract(credited)
        );
    }

    /**
     * Calcula o saldo total de ajustes para uma cobrança
     * 
     * INCREASE: soma
     * DECREASE: subtrai
     */
    public BigDecimal adjustmentBalance(UUID receivableId) {

        return MoneyRules.normalize(
                receivableAdjustmentRepository
                        .calculateAdjustmentBalance(receivableId)
        );
    }

    /**
     * Valor efetivo da cobrança incluindo ajustes
     * 
     * effectiveReceivableAmount = netAmount + adjustmentBalance
     * 
     * Validação: não pode ser negativo
     * @throws IllegalStateException se o valor efetivo for negativo
     */
    public BigDecimal effectiveReceivableAmount(Receivable receivable) {

        BigDecimal effective = MoneyRules.normalize(
                receivable
                        .getNetAmount()
                        .add(
                                adjustmentBalance(
                                        receivable.getId()
                                )
                        )
        );

        if (effective.signum() < 0) {
            throw new IllegalStateException(
                    "Valor efetivo da cobrança ficou negativo: " + effective
            );
        }

        return effective;
    }
}
