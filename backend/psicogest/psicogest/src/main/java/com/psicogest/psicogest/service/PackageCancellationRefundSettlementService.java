package com.psicogest.psicogest.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

import com.psicogest.psicogest.model.entity.PatientPackageCancellation;
import com.psicogest.psicogest.model.entity.Refund;
import com.psicogest.psicogest.model.entity.Refund.RefundStatus;
import com.psicogest.psicogest.repository.PatientPackageCancellationRepository;
import com.psicogest.psicogest.repository.RefundRepository;

/**
 * Service para avaliar e completar settlement de refunds em cancelamentos
 * 
 * Quando todos os refunds de um cancelamento estão confirmados,
 * marca o cancelamento como completo e atualiza o status do pacote.
 * 
 * É acionado por um event handler que escuta REFUND_CONFIRMED.
 */
@Service
@Transactional
public class PackageCancellationRefundSettlementService {

    private final PatientPackageCancellationRepository cancellationRepository;
    private final RefundRepository refundRepository;
    private final FinanceBalanceService financeBalanceService;
    private final Clock clock;

    public PackageCancellationRefundSettlementService(
            PatientPackageCancellationRepository cancellationRepository,
            RefundRepository refundRepository,
            FinanceBalanceService financeBalanceService,
            Clock clock
    ) {
        this.cancellationRepository = cancellationRepository;
        this.refundRepository = refundRepository;
        this.financeBalanceService = financeBalanceService;
        this.clock = clock;
    }

    /**
     * Avalia se todos os refunds de um cancelamento foram confirmados
     * 
     * Se sim:
     * 1. Verifica se não há mais pagamento excedente
     * 2. Marca cancelamento como COMPLETED
     * 3. Marca pacote como CANCELLED
     * 
     * Se não ou há erro:
     * 1. Retorna sem fazer nada (idempotente)
     * 
     * @param cancellationId ID do cancelamento a avaliar
     * @throws IllegalStateException se ainda há pagamento excedente
     */
    public void evaluateRefundSettlement(
            UUID cancellationId
    ) {

        // Busca cancelamento com lock pessimista
        PatientPackageCancellation cancellation =
                cancellationRepository
                        .findByIdForUpdate(cancellationId)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Cancelamento não encontrado: "
                                                + cancellationId
                                )
                        );

        // Busca todos os refunds deste cancelamento
        List<Refund> refunds =
                refundRepository
                        .findByPatientPackageCancellationId(
                                cancellationId
                        );

        // Verifica se TODOS os refunds estão confirmados
        boolean allConfirmed = refunds.stream()
                .allMatch(
                        refund ->
                                refund.getStatus()
                                        == RefundStatus.CONFIRMED
                );

        // Guard: se não todos confirmados, nada a fazer
        if (!allConfirmed) {
            return;
        }

        // Verifica se não há mais pagamento excedente
        // (todos os refunds devem ter revertido qualquer overpayment)
        // TODO: Buscar receivable através do payment/refund
        // Por ora, assumindo que será passado via outro meio
        // ou que a verificação será feita no nível do payment
        
        // Placeholder: implementar após definir relacionamento entre
        // PatientPackage e Receivable
        BigDecimal remainingOverpayment = BigDecimal.ZERO;
        /*
        BigDecimal remainingOverpayment =
                financeBalanceService
                        .overpaidAmount(
                                receivable
                        );
        */

        // Se ainda há overpayment, há inconsistência
        if (remainingOverpayment.signum() != 0) {

            throw new IllegalStateException(
                    "Cancelamento ainda possui pagamento excedente: "
                            + remainingOverpayment
            );
        }

        // Marca cancelamento como COMPLETED
        cancellation.complete(clock.instant());

        // Marca pacote como CANCELLED
        cancellation
                .getPatientPackage()
                .cancel(clock.instant());

        // Persiste ambas as alterações
        cancellationRepository.saveAndFlush(cancellation);
    }
}
