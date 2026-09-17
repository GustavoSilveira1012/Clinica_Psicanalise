package com.psicogest.psicogest.service.bank;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.psicogest.psicogest.dto.IgnoreBankTransactionDTO;
import com.psicogest.psicogest.dto.ReconcilePaymentDTO;
import com.psicogest.psicogest.dto.ReconciliationSuggestionDTO;
import com.psicogest.psicogest.exception.FinanceConflictException;
import com.psicogest.psicogest.exception.FinanceValidationException;
import com.psicogest.psicogest.exception.ResourceNotFoundException;
import com.psicogest.psicogest.infrastructure.bank.parser.BankTransactionDirection;
import com.psicogest.psicogest.model.enums.ReconciliationConfidence;
import com.psicogest.psicogest.model.entity.BankReconciliationAllocation;
import com.psicogest.psicogest.model.entity.BankReconciliationAllocation.AllocationSource;
import com.psicogest.psicogest.model.entity.BankReconciliationAllocation.AllocationStatus;
import com.psicogest.psicogest.model.entity.BankTransaction;
import com.psicogest.psicogest.model.entity.Payment;
import com.psicogest.psicogest.model.entity.Payment.PaymentStatus;
import com.psicogest.psicogest.model.entity.ProviderSettlement;
import com.psicogest.psicogest.repository.BankReconciliationAllocationRepository;
import com.psicogest.psicogest.repository.BankTransactionRepository;
import com.psicogest.psicogest.repository.PaymentRepository;
import com.psicogest.psicogest.repository.ProviderSettlementRepository;
import com.psicogest.psicogest.security.SecurityActor;
import com.psicogest.psicogest.security.audit.AuditAction;
import com.psicogest.psicogest.security.audit.AuditCommand;
import com.psicogest.psicogest.security.audit.AuditOutcome;
import com.psicogest.psicogest.security.audit.AuditService;

import lombok.extern.slf4j.Slf4j;

/**
 * Serviço de reconciliação bancária manual
 * 
 * Fluxo:
 * 1. reconcileWithPayment: PESSIMISTIC_WRITE lock BankTransaction → Payment
 * 2. Validações: CREDIT, mesma Clinic, saldos
 * 3. Cria BankReconciliationAllocation
 * 4. Atualiza status BankTransaction
 * 5. Auditoria
 * 6. COMMIT
 */
@Slf4j
@Service
@Transactional
public class BankReconciliationService {

    private final BankTransactionRepository bankTransactionRepository;

    private final PaymentRepository paymentRepository;

    private final BankReconciliationAllocationRepository allocationRepository;

    private final ProviderSettlementRepository providerSettlementRepository;

    private final AuditService auditService;

    private final Clock clock;

    public BankReconciliationService(
            BankTransactionRepository bankTransactionRepository,
            PaymentRepository paymentRepository,
            BankReconciliationAllocationRepository allocationRepository,
            ProviderSettlementRepository providerSettlementRepository,
            AuditService auditService,
            Clock clock
    ) {
        this.bankTransactionRepository = bankTransactionRepository;
        this.paymentRepository = paymentRepository;
        this.allocationRepository = allocationRepository;
        this.providerSettlementRepository = providerSettlementRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    /**
     * Reconcilia lançamento bancário com Payment
     */
    public BankReconciliationAllocation reconcileWithPayment(
            UUID transactionId,
            ReconcilePaymentDTO dto,
            SecurityActor actor
    ) {

        Instant now = clock.instant();

        // Lock BankTransaction
        BankTransaction transaction =
                bankTransactionRepository
                        .findByIdForUpdate(transactionId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Lançamento não encontrado"
                                        )
                        );

        // Lock Payment
        Payment payment =
                paymentRepository
                        .findByIdForUpdate(dto.paymentId())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Pagamento não encontrado"
                                        )
                        );

        // Validações
        if (transaction.getDirection() !=
                BankTransactionDirection.CREDIT) {

            throw new FinanceConflictException(
                    "Apenas créditos podem ser conciliados"
            );
        }

        if (!transaction.getBankAccount()
                .getClinic()
                .getId()
                .equals(
                        payment.getClinic()
                                .getId()
                )) {

            throw new FinanceConflictException(
                    "Lançamento e pagamento de clínicas diferentes"
            );
        }

        // Saldo bancário
        BigDecimal reconciled =
                allocationRepository
                        .sumReconciledAmount(
                                transactionId
                        );

        if (dto.amount()
                .compareTo(
                        transaction.getAmount()
                                .subtract(reconciled)
                ) > 0) {

            throw new FinanceConflictException(
                    "Saldo insuficiente"
            );
        }

        // Saldo do payment
        BigDecimal paymentReconciled =
                allocationRepository
                        .sumReconciledForPayment(
                                payment.getId()
                        );

        if (dto.amount()
                .compareTo(
                        payment.getAmount()
                                .subtract(
                                        paymentReconciled
                                )
                ) > 0) {

            throw new FinanceConflictException(
                    "Pagamento sem saldo"
            );
        }

        // Criar allocation
        BankReconciliationAllocation allocation =
                BankReconciliationAllocation
                        .builder()

                        .id(UUID.randomUUID())

                        .bankTransaction(transaction)

                        .payment(payment)

                        .allocatedAmount(dto.amount())

                        .currency(
                                transaction
                                        .getCurrency()
                        )

                        .status(
                                AllocationStatus.CONFIRMED
                        )

                        .allocatedBy(
                                AllocationSource.MANUAL
                        )

                        .allocatedAt(now)

                        .build();

        allocationRepository.save(allocation);

        // Atualizar status
        BigDecimal newReconciled =
                reconciled.add(dto.amount());

        if (newReconciled.compareTo(
                transaction.getAmount()) == 0) {

            transaction.markReconciled();

        } else {

            transaction
                    .markPartiallyReconciled();
        }

        bankTransactionRepository.save(transaction);

        // Auditoria
        auditService.recordCriticalWrite(

                new AuditCommand(

                        actor.userId(),

                        actor.sessionId(),

                        newReconciled.compareTo(
                                transaction.getAmount()
                        ) == 0
                                ? AuditAction
                                .BANK_TRANSACTION_RECONCILED
                                : AuditAction
                                .BANK_TRANSACTION_PARTIALLY_RECONCILED,

                        "BANK_TRANSACTION",

                        transaction.getId()
                                .toString(),

                        null,

                        transaction.getBankAccount()
                                .getClinic()
                                .getId(),

                        AuditOutcome.SUCCESS,

                        actor.correlationId(),

                        actor.sourceIp(),

                        actor.userAgentHash(),

                        Map.of(
                                "paymentId",
                                payment.getId()
                                        .toString(),

                                "amount",
                                dto.amount()
                                        .toPlainString()
                        )
                )
        );

        log.info(
                "Reconciliado: " +
                        "tx={}, payment={}, amount={}",
                transactionId,
                payment.getId(),
                dto.amount()
        );

        return allocation;
    }

    /**
     * Lista sugestões de reconciliação
     */
    public List<ReconciliationSuggestionDTO>
            suggestReconciliations(
                    UUID transactionId
            ) {
        BankTransaction transaction = bankTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Lançamento não encontrado"));

        if (transaction.getDirection() != BankTransactionDirection.CREDIT
                || transaction.getIgnoredAt() != null) {
            return List.of();
        }

        BigDecimal alreadyReconciled = allocationRepository.sumReconciledAmount(transactionId);
        BigDecimal remaining = transaction.getAmount().subtract(alreadyReconciled);
        if (remaining.signum() <= 0 || transaction.getBankAccount().getClinic() == null) {
            return List.of();
        }

        LocalDate bookingDate = transaction.getBookingDate();
        Long clinicId = transaction.getBankAccount().getClinic().getId();
        return paymentRepository.findConfirmedByClinicId(clinicId).stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.CONFIRMED
                        || payment.getStatus() == PaymentStatus.PARTIALLY_REFUNDED)
                .map(payment -> suggestionFor(transaction, payment, remaining, bookingDate))
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparingInt((ReconciliationSuggestionDTO suggestion) -> confidenceRank(suggestion.confidence()))
                        .thenComparing(ReconciliationSuggestionDTO::paymentDate, Comparator.reverseOrder()))
                .limit(10)
                .toList();
    }

    private ReconciliationSuggestionDTO suggestionFor(BankTransaction transaction, Payment payment,
                                                       BigDecimal remaining, LocalDate bookingDate) {
        BigDecimal allocated = allocationRepository.sumReconciledForPayment(payment.getId());
        BigDecimal available = payment.getAmount().subtract(allocated);
        if (available.signum() <= 0) return null;

        BigDecimal amount = remaining.min(available);
        if (amount.signum() <= 0) return null;
        LocalDate paymentDate = payment.getReceivedAt().atZone(clock.getZone()).toLocalDate();
        long dayDifference = Math.abs(java.time.temporal.ChronoUnit.DAYS.between(bookingDate, paymentDate));
        boolean exactAmount = available.compareTo(remaining) == 0 || payment.getAmount().compareTo(transaction.getAmount()) == 0;
        ReconciliationConfidence confidence = exactAmount && dayDifference <= 1
                ? ReconciliationConfidence.HIGH
                : exactAmount && dayDifference <= 7
                        ? ReconciliationConfidence.MEDIUM
                        : dayDifference <= 7 ? ReconciliationConfidence.LOW : null;
        if (confidence == null) return null;
        String reason = exactAmount
                ? "Valor compatível; data do pagamento está a " + dayDifference + " dia(s) do lançamento"
                : "Pagamento próximo da data do lançamento; confirme o valor alocado";
        return new ReconciliationSuggestionDTO(payment.getId(), amount, paymentDate,
                payment.getPaymentMethod(), confidence, reason);
    }

    private int confidenceRank(ReconciliationConfidence confidence) {
        return switch (confidence) {
            case HIGH -> 0;
            case MEDIUM -> 1;
            case LOW -> 2;
        };
    }

    /**
     * Ignora lançamento
     */
    public void ignoreBankTransaction(
            UUID transactionId,
            IgnoreBankTransactionDTO dto,
            SecurityActor actor
    ) {

        Instant now = clock.instant();

        BankTransaction transaction =
                bankTransactionRepository
                        .findById(transactionId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Lançamento não encontrado"
                                        )
                        );

        if (transaction.getIgnoredAt() != null) {

            throw new FinanceValidationException(
                    "Já ignorado"
            );
        }

        transaction.setIgnoredAt(now);
        transaction.setIgnoredBy(actor.userId());
        transaction.setIgnoreReason(dto.reason());

        bankTransactionRepository.save(transaction);

        auditService.recordCriticalWrite(

                new AuditCommand(

                        actor.userId(),

                        actor.sessionId(),

                        AuditAction
                                .BANK_TRANSACTION_IGNORED,

                        "BANK_TRANSACTION",

                        transaction.getId()
                                .toString(),

                        null,

                        transaction.getBankAccount()
                                .getClinic()
                                .getId(),

                        AuditOutcome.SUCCESS,

                        actor.correlationId(),

                        actor.sourceIp(),

                        actor.userAgentHash(),

                        Map.of(
                                "reason",
                                dto.reason()
                        )
                )
        );

        log.info(
                "Ignorado: tx={}, reason={}",
                transactionId,
                dto.reason()
        );
    }

    /**
     * Reconcilia lançamento bancário com ProviderSettlement
     * 
     * Validações:
     * - Settlement deve ser SETTLED
     * - Settlement deve estar MATCHED
     * - Direção do settlement deve bater com direção do bank transaction
     * - Clinic do settlement deve ser da conta
     * - Saldo do settlement não pode ser liquidado acima
     */
    public BankReconciliationAllocation reconcileWithProviderSettlement(
            UUID transactionId,
            UUID settlementId,
            BigDecimal amount,
            SecurityActor actor
    ) {

        Instant now = clock.instant();

        // Lock BankTransaction
        BankTransaction transaction =
                bankTransactionRepository
                        .findByIdForUpdate(transactionId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Lançamento não encontrado"
                                        )
                        );

        // Lock ProviderSettlement
        ProviderSettlement settlement =
                providerSettlementRepository
                        .findByIdForUpdate(settlementId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Repasse não encontrado"
                                        )
                        );

        // Validar que settlement está pronto
        if (!settlement.getStatus()
                .name()
                .equals("SETTLED")) {

            throw new FinanceConflictException(
                    "Repasse não foi liquidado"
            );
        }

        // Validar que foi validado
        if (!settlement.getValidationStatus()
                .name()
                .equals("MATCHED")) {

            throw new FinanceConflictException(
                    "Repasse possui divergência de saldo"
            );
        }

        // Validar Clinic
        if (!transaction.getBankAccount()
                .getClinic()
                .getId()
                .equals(
                        settlement.getClinic()
                                .getId()
                )) {

            throw new FinanceConflictException(
                    "Settlement pertence a outra clínica"
            );
        }

        // Validar direção
        // Se settlement é CREDIT → BankTransaction deve ser CREDIT
        // Se settlement é DEBIT → BankTransaction deve ser DEBIT
        String settlementDirection =
                settlement.getSettlementDirection()
                        .name();

        BankTransactionDirection txDirection =
                transaction.getDirection();

        boolean directionValid = false;

        if ("CREDIT_TO_FINANCIAL_ENTITY"
                .equals(settlementDirection)) {

            directionValid =
                    txDirection ==
                    BankTransactionDirection.CREDIT;

        } else if (
                "DEBIT_FROM_FINANCIAL_ENTITY"
                        .equals(settlementDirection)
        ) {

            directionValid =
                    txDirection ==
                    BankTransactionDirection.DEBIT;
        }

        if (!directionValid) {

            throw new FinanceConflictException(
                    "Direção do settlement não bate com direção do lançamento"
            );
        }

        // Saldo bancário
        BigDecimal reconciled =
                allocationRepository
                        .sumReconciledAmount(
                                transactionId
                        );

        if (amount.compareTo(
                transaction.getAmount()
                        .subtract(reconciled)
        ) > 0) {

            throw new FinanceConflictException(
                    "Saldo insuficiente no lançamento"
            );
        }

        // Saldo do settlement (simplificado:
        // assumir que o amount é <= reportedNetAmount)
        if (amount.compareTo(
                settlement.getReportedNetAmount()
        ) > 0) {

            throw new FinanceConflictException(
                    "Saldo insuficiente no repasse"
            );
        }

        // Criar allocation
        BankReconciliationAllocation allocation =
                BankReconciliationAllocation
                        .builder()

                        .id(UUID.randomUUID())

                        .bankTransaction(transaction)

                        .providerSettlement(
                                settlement
                        )

                        .allocatedAmount(amount)

                        .currency(
                                transaction
                                        .getCurrency()
                        )

                        .status(
                                AllocationStatus.CONFIRMED
                        )

                        .allocatedBy(
                                AllocationSource.MANUAL
                        )

                        .allocatedAt(now)

                        .createdAt(now)

                        .build();

        allocationRepository.save(allocation);

        // Atualizar status BankTransaction
        BigDecimal newReconciled =
                reconciled.add(amount);

        if (newReconciled.compareTo(
                transaction.getAmount()) == 0) {

            transaction.markReconciled();

        } else {

            transaction
                    .markPartiallyReconciled();
        }

        bankTransactionRepository.save(transaction);

        // Auditoria
        auditService.recordCriticalWrite(

                new AuditCommand(

                        actor.userId(),

                        actor.sessionId(),

                        newReconciled.compareTo(
                                transaction.getAmount()
                        ) == 0
                                ? AuditAction
                                .BANK_TRANSACTION_RECONCILED
                                : AuditAction
                                .BANK_TRANSACTION_PARTIALLY_RECONCILED,

                        "BANK_TRANSACTION",

                        transaction.getId()
                                .toString(),

                        null,

                        transaction.getBankAccount()
                                .getClinic()
                                .getId(),

                        AuditOutcome.SUCCESS,

                        actor.correlationId(),

                        actor.sourceIp(),

                        actor.userAgentHash(),

                        Map.of(
                                "settlementId",
                                settlement.getId()
                                        .toString(),

                                "amount",
                                amount.toPlainString()
                        )
                )
        );

        log.info(
                "Settlement reconciliado: " +
                        "tx={}, settlement={}, amount={}",
                transactionId,
                settlementId,
                amount
        );

        return allocation;
    }
}
