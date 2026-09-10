package com.psicogest.psicogest.service.settlement;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.psicogest.psicogest.exception.FinanceConflictException;
import com.psicogest.psicogest.exception.FinanceValidationException;
import com.psicogest.psicogest.exception.ResourceNotFoundException;
import com.psicogest.psicogest.infrastructure.bank.parser.BankTransactionDirection;
import com.psicogest.psicogest.model.entity.Payment;
import com.psicogest.psicogest.model.entity.ProviderSettlement;
import com.psicogest.psicogest.model.entity.ProviderSettlementItem;
import com.psicogest.psicogest.model.entity.Refund;
import com.psicogest.psicogest.model.enums.ProviderSettlementDirection;
import com.psicogest.psicogest.model.enums.ProviderSettlementItemType;
import com.psicogest.psicogest.model.enums.ProviderSettlementStatus;
import com.psicogest.psicogest.model.enums.SettlementEntryDirection;
import com.psicogest.psicogest.model.vo.SettlementNet;
import com.psicogest.psicogest.repository.PaymentRepository;
import com.psicogest.psicogest.repository.ProviderSettlementItemRepository;
import com.psicogest.psicogest.repository.ProviderSettlementRepository;
import com.psicogest.psicogest.repository.RefundRepository;
import com.psicogest.psicogest.security.SecurityActor;
import com.psicogest.psicogest.security.audit.AuditAction;
import com.psicogest.psicogest.security.audit.AuditCommand;
import com.psicogest.psicogest.security.audit.AuditOutcome;
import com.psicogest.psicogest.security.audit.AuditService;

import lombok.extern.slf4j.Slf4j;

/**
 * Serviço de gestão de repasses de gateway
 * 
 * Fluxo:
 * 1. RECEIVED: informações básicas do gateway
 * 2. addItems(): adiciona pagamentos, refunds, taxas
 * 3. validateSettlement(): compara saldo calculado vs reportado
 *    - Se bater → VALIDATED (MATCHED)
 *    - Se divergir → MISMATCHED
 * 4. markSettled(): gateway confirmou o pagamento → SETTLED
 * 5. reconcileWithBankTransaction(): banco confirma entrada
 */
@Slf4j
@Service
@Transactional
public class ProviderSettlementService {

    private final ProviderSettlementRepository settlementRepository;

    private final ProviderSettlementItemRepository itemRepository;

    private final PaymentRepository paymentRepository;

    private final RefundRepository refundRepository;

    private final ProviderSettlementBalanceService balanceService;

    private final AuditService auditService;

    private final Clock clock;

    public ProviderSettlementService(
            ProviderSettlementRepository settlementRepository,
            ProviderSettlementItemRepository itemRepository,
            PaymentRepository paymentRepository,
            RefundRepository refundRepository,
            ProviderSettlementBalanceService balanceService,
            AuditService auditService,
            Clock clock
    ) {
        this.settlementRepository = settlementRepository;
        this.itemRepository = itemRepository;
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.balanceService = balanceService;
        this.auditService = auditService;
        this.clock = clock;
    }

    /**
     * Valida saldo do repasse
     * 
     * Compara:
     * - Saldo calculado (soma dos itens)
     * - Saldo informado pelo gateway
     * 
     * Se bater → VALIDATED (MATCHED)
     * Se divergir → MISMATCHED (sem transição de status)
     */
    public void validateSettlement(
            UUID settlementId,
            SecurityActor actor
    ) {

        Instant now = clock.instant();

        // Lock settlement
        ProviderSettlement settlement =
                settlementRepository
                        .findByIdForUpdate(settlementId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Repasse não encontrado"
                                        )
                        );

        // Calcular saldo
        SettlementNet calculated =
                balanceService.calculateNet(
                        settlementId
                );

        // Validar direção
        boolean directionMatches =
                calculated.direction()
                        == settlement
                        .getSettlementDirection();

        // Validar valor
        boolean valueMatches =
                calculated.amount()
                        .compareTo(
                                settlement
                                .getReportedNetAmount()
                        ) == 0;

        // Aplicar validação
        if (directionMatches && valueMatches) {

            settlement.markValidated(now);

            auditService.recordCriticalWrite(

                    new AuditCommand(

                            actor.userId(),

                            actor.sessionId(),

                            AuditAction
                                    .PROVIDER_SETTLEMENT_VALIDATED,

                            "PROVIDER_SETTLEMENT",

                            settlement.getId()
                                    .toString(),

                            null,

                            settlement.getClinic()
                                    .getId(),

                            AuditOutcome.SUCCESS,

                            actor.correlationId(),

                            actor.sourceIp(),

                            actor.userAgentHash(),

                            Map.of(
                                    "direction",
                                    calculated.direction()
                                            .name(),

                                    "amount",
                                    calculated.amount()
                                            .toPlainString()
                            )
                    )
            );

            log.info(
                    "Repasse validado: " +
                            "settlementId={}, amount={}, direction={}",
                    settlementId,
                    calculated.amount(),
                    calculated.direction()
            );

        } else {

            settlement.markMismatch(now);

            auditService.recordCriticalWrite(

                    new AuditCommand(

                            actor.userId(),

                            actor.sessionId(),

                            AuditAction
                                    .PROVIDER_SETTLEMENT_MISMATCH_DETECTED,

                            "PROVIDER_SETTLEMENT",

                            settlement.getId()
                                    .toString(),

                            null,

                            settlement.getClinic()
                                    .getId(),

                            AuditOutcome.FAILURE,

                            actor.correlationId(),

                            actor.sourceIp(),

                            actor.userAgentHash(),

                            Map.of(
                                    "calculated_direction",
                                    calculated.direction()
                                            .name(),

                                    "calculated_amount",
                                    calculated.amount()
                                            .toPlainString(),

                                    "reported_direction",
                                    settlement
                                            .getSettlementDirection()
                                            .name(),

                                    "reported_amount",
                                    settlement
                                            .getReportedNetAmount()
                                            .toPlainString(),

                                    "direction_match",
                                    String.valueOf(
                                            directionMatches
                                    ),

                                    "value_match",
                                    String.valueOf(
                                            valueMatches
                                    )
                            )
                    )
            );

            log.warn(
                    "Divergência detectada: " +
                            "settlementId={}, " +
                            "directionMatch={}, " +
                            "valueMatch={}",
                    settlementId,
                    directionMatches,
                    valueMatches
            );
        }

        settlementRepository.save(settlement);
    }

    /**
     * Adiciona itens a um repasse
     * 
     * Com validações:
     * - Payment deve pertencer à mesma Clinic
     * - Payment deve ser do mesmo provider
     * - Não pode liquidar acima do valor
     * 
     * Usa locks para evitar race conditions:
     * - Lock Settlement
     * - Lock Payments/Refunds por UUID (ordenado)
     */
    public void addItems(
            UUID settlementId,
            List<ProviderSettlementItem> items,
            SecurityActor actor
    ) {

        // Lock settlement
        ProviderSettlement settlement =
                settlementRepository
                        .findByIdForUpdate(settlementId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Repasse não encontrado"
                                        )
                        );

        for (ProviderSettlementItem item : items) {

            // Validar Payment
            if (item.getItemType() ==
                    ProviderSettlementItemType.PAYMENT) {

                Payment payment = item.getPayment();

                if (payment == null) {

                    throw new FinanceValidationException(
                            "Payment obrigatório para item PAYMENT"
                    );
                }

                // Validar Clinic
                if (!payment.getClinic()
                        .getId()
                        .equals(
                                settlement.getClinic()
                                        .getId()
                        )) {

                    throw new FinanceConflictException(
                            "Pagamento pertence a outra clínica"
                    );
                }

                // Validar Provider
                if (!payment.getProvider()
                        .equals(
                                settlement.getProvider()
                                .name()
                        )) {

                    throw new FinanceConflictException(
                            "Pagamento pertence a outro provedor"
                    );
                }

                // Validar saldo
                BigDecimal alreadySettled =
                        itemRepository
                                .sumSettledForPayment(
                                        payment.getId()
                                );

                BigDecimal remaining =
                        payment.getAmount()
                                .subtract(
                                        alreadySettled
                                );

                if (item.getAmount()
                        .compareTo(remaining) > 0) {

                    throw new FinanceConflictException(
                            "Saldo insuficiente do pagamento: " +
                                    "restante=" +
                                    remaining
                    );
                }
            }

            // Validar Refund
            if (item.getItemType() ==
                    ProviderSettlementItemType.REFUND) {

                Refund refund = item.getRefund();

                if (refund == null) {

                    throw new FinanceValidationException(
                            "Refund obrigatório para item REFUND"
                    );
                }

                // Validar saldo
                BigDecimal alreadySettled =
                        itemRepository
                                .sumSettledForRefund(
                                        refund.getId()
                                );

                BigDecimal remaining =
                        refund.getAmount()
                                .subtract(
                                        alreadySettled
                                );

                if (item.getAmount()
                        .compareTo(remaining) > 0) {

                    throw new FinanceConflictException(
                            "Saldo insuficiente do refund"
                    );
                }
            }

            // Salvar item
            item.setSettlement(settlement);

            itemRepository.save(item);
        }

        log.info(
                "Itens adicionados ao repasse: " +
                        "settlementId={}, count={}",
                settlementId,
                items.size()
        );
    }

    /**
     * Marca repasse como liquidado (SETTLED)
     * 
     * Pré-requisitos:
     * - Status deve ser VALIDATED
     * - ValidationStatus deve ser MATCHED
     * 
     * Transição: VALIDATED → SETTLED
     */
    public void markSettled(
            UUID settlementId,
            Instant settledAt,
            SecurityActor actor
    ) {

        ProviderSettlement settlement =
                settlementRepository
                        .findByIdForUpdate(settlementId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Repasse não encontrado"
                                        )
                        );

        // Transition
        settlement.markSettled(settledAt);

        settlementRepository.save(settlement);

        // Auditoria
        auditService.recordCriticalWrite(

                new AuditCommand(

                        actor.userId(),

                        actor.sessionId(),

                        AuditAction
                                .PROVIDER_SETTLEMENT_SETTLED,

                        "PROVIDER_SETTLEMENT",

                        settlement.getId()
                                .toString(),

                        null,

                        settlement.getClinic()
                                .getId(),

                        AuditOutcome.SUCCESS,

                        actor.correlationId(),

                        actor.sourceIp(),

                        actor.userAgentHash(),

                        Map.of(
                                "settled_at",
                                settledAt.toString()
                        )
                )
        );

        log.info(
                "Repasse liquidado: " +
                        "settlementId={}, settledAt={}",
                settlementId,
                settledAt
        );
    }

    /**
     * Sincroniza status de reconciliação
     * 
     * Após conciliação bancária, atualiza status
     * do settlement com base no progresso
     */
    public void synchronizeReconciliationStatus(
            ProviderSettlement settlement,
            BigDecimal reconciled,
            BigDecimal netAmount
    ) {

        Instant now = clock.instant();

        int comparison =
                reconciled.compareTo(netAmount);

        if (comparison > 0) {

            throw new IllegalStateException(
                    "Repasse conciliado acima do valor líquido"
            );
        }

        if (comparison == 0) {

            settlement.markReconciled(now);

            return;
        }

        settlement.markPartiallyReconciled(now);
    }
}
