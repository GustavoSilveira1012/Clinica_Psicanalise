package com.psicogest.psicogest.service.bank;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
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
import com.psicogest.psicogest.model.entity.BankReconciliationAllocation;
import com.psicogest.psicogest.model.entity.BankReconciliationAllocation.AllocationSource;
import com.psicogest.psicogest.model.entity.BankReconciliationAllocation.AllocationStatus;
import com.psicogest.psicogest.model.entity.BankTransaction;
import com.psicogest.psicogest.model.entity.Payment;
import com.psicogest.psicogest.repository.BankReconciliationAllocationRepository;
import com.psicogest.psicogest.repository.BankTransactionRepository;
import com.psicogest.psicogest.repository.PaymentRepository;
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

    private final AuditService auditService;

    private final Clock clock;

    public BankReconciliationService(
            BankTransactionRepository bankTransactionRepository,
            PaymentRepository paymentRepository,
            BankReconciliationAllocationRepository allocationRepository,
            AuditService auditService,
            Clock clock
    ) {
        this.bankTransactionRepository = bankTransactionRepository;
        this.paymentRepository = paymentRepository;
        this.allocationRepository = allocationRepository;
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

        return new ArrayList<>();
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
}
