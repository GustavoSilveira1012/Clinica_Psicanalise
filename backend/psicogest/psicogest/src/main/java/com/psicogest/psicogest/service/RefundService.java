package com.psicogest.psicogest.service;

import com.psicogest.psicogest.domain.finance.MoneyRules;
import com.psicogest.psicogest.domain.finance.RefundStateMachine;
import com.psicogest.psicogest.dto.RefundAllocationRequestDTO;
import com.psicogest.psicogest.dto.RefundCreateDTO;
import com.psicogest.psicogest.exception.FinanceConflictException;
import com.psicogest.psicogest.exception.FinanceValidationException;
import com.psicogest.psicogest.exception.ResourceNotFoundException;
import com.psicogest.psicogest.infrastructure.payment.provider.PaymentProviderType;
import com.psicogest.psicogest.model.entity.Payment;
import com.psicogest.psicogest.model.entity.Payment.PaymentStatus;
import com.psicogest.psicogest.model.entity.PaymentAllocation;
import com.psicogest.psicogest.model.entity.Receivable;
import com.psicogest.psicogest.model.entity.Refund;
import com.psicogest.psicogest.model.entity.Refund.RefundStatus;
import com.psicogest.psicogest.model.entity.RefundAllocation;
import com.psicogest.psicogest.repository.PaymentAllocationRepository;
import com.psicogest.psicogest.repository.PaymentRepository;
import com.psicogest.psicogest.repository.ReceivableRepository;
import com.psicogest.psicogest.repository.RefundAllocationRepository;
import com.psicogest.psicogest.repository.RefundRepository;
import com.psicogest.psicogest.security.SecurityActor;
import com.psicogest.psicogest.security.SecurityHashService;
import com.psicogest.psicogest.security.audit.AuditAction;
import com.psicogest.psicogest.security.audit.AuditCommand;
import com.psicogest.psicogest.security.audit.AuditOutcome;
import com.psicogest.psicogest.security.audit.AuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Service para operações com reembolsos
 * 
 * 23-30. Criar reembolso com validações
 * 32. Auditoria de solicitação
 * 33-37. Confirmar reembolso
 */
@Slf4j
@Service
@Transactional
public class RefundService {

    private final PaymentRepository paymentRepository;
    private final PaymentAllocationRepository allocationRepository;
    private final RefundRepository refundRepository;
    private final RefundAllocationRepository refundAllocationRepository;
    private final ReceivableRepository receivableRepository;
    private final FinanceBalanceService balanceService;
    private final AuditService auditService;
    private final SecurityHashService hashService;
    private final RefundStateMachine refundStateMachine;
    private final Clock clock;

    public RefundService(
            PaymentRepository paymentRepository,
            PaymentAllocationRepository allocationRepository,
            RefundRepository refundRepository,
            RefundAllocationRepository refundAllocationRepository,
            ReceivableRepository receivableRepository,
            FinanceBalanceService balanceService,
            AuditService auditService,
            SecurityHashService hashService,
            RefundStateMachine refundStateMachine,
            Clock clock
    ) {
        this.paymentRepository = paymentRepository;
        this.allocationRepository = allocationRepository;
        this.refundRepository = refundRepository;
        this.refundAllocationRepository = refundAllocationRepository;
        this.receivableRepository = receivableRepository;
        this.balanceService = balanceService;
        this.auditService = auditService;
        this.hashService = hashService;
        this.refundStateMachine = refundStateMachine;
        this.clock = clock;
    }

    /**
     * 23-30. Cria reembolso com validações completas
     * 32. Auditoria de solicitação
     * 
     * POST /payments/{paymentId}/refunds
     * 
     * @param paymentId ID do pagamento
     * @param rawIdempotencyKey chave de idempotência
     * @param dto dados do refund
     * @param actor usuário que fez a requisição
     * @return refund criado
     */
    @Transactional
    public Refund create(
            UUID paymentId,
            String rawIdempotencyKey,
            RefundCreateDTO dto,
            SecurityActor actor
    ) {

        // Normalizar idempotency-key
        String idempotencyKey =
                normalizeIdempotencyKey(rawIdempotencyKey);

        // Buscar payment com lock
        Payment payment =
                paymentRepository
                        .findByIdForUpdate(paymentId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Pagamento não encontrado"
                                        )
                        );

        // Validar status
        if (
                payment.getStatus() != PaymentStatus.CONFIRMED
                &&
                payment.getStatus() != PaymentStatus.PARTIALLY_REFUNDED
        ) {

            throw new FinanceConflictException(
                    "Pagamento não permite devolução"
            );
        }

        // Validar que não existe refund PENDING
        if (
                refundRepository
                        .existsByPaymentIdAndStatus(
                                paymentId,
                                RefundStatus.PENDING
                        )
        ) {

            throw new FinanceConflictException(
                    "Já existe uma devolução pendente para este pagamento"
            );
        }

        // 23. Validar valor máximo reembolsável
        BigDecimal alreadyRefunded =
                balanceService
                        .confirmedRefundAmount(paymentId);

        BigDecimal refundable =
                payment.getAmount()
                        .subtract(alreadyRefunded);

        BigDecimal refundAmount =
                MoneyRules.normalize(dto.amount());

        if (
                refundAmount.compareTo(refundable) > 0
        ) {

            throw new FinanceConflictException(
                    "Valor da devolução excede o valor disponível para reembolso"
            );
        }

        // 24. Quanto está livre para alocar?
        BigDecimal unallocated =
                balanceService
                        .availablePaymentAmount(payment);

        BigDecimal requiredReversal =
                refundAmount
                        .subtract(unallocated)
                        .max(BigDecimal.ZERO);

        // 25. Ordenar allocations para lock (reduz deadlock)
        List<RefundAllocationRequestDTO> lines =
                dto.allocations()
                        .stream()
                        .sorted(
                                Comparator.comparing(
                                        RefundAllocationRequestDTO
                                                ::paymentAllocationId
                                )
                        )
                        .toList();

        // 26-27. Validar cada allocation
        BigDecimal totalReversal =
                BigDecimal.ZERO.setScale(2);

        for (RefundAllocationRequestDTO line : lines) {

            // 26. Lock allocation nessa ordem
            PaymentAllocation allocation =
                    allocationRepository
                            .findByIdForUpdate(
                                    line.paymentAllocationId()
                            )
                            .orElseThrow(
                                    () ->
                                            new ResourceNotFoundException(
                                                    "Alocação financeira não encontrada"
                                            )
                            );

            // 26. Pertence ao payment?
            if (
                    !allocation
                            .getPayment()
                            .getId()
                            .equals(paymentId)
            ) {

                throw new FinanceConflictException(
                        "A alocação não pertence ao pagamento"
                );
            }

            // 27. Quanto pode ser revertido?
            BigDecimal previouslyRefunded =
                    refundAllocationRepository
                            .sumConfirmedRefundedAmount(
                                    allocation.getId()
                            );

            BigDecimal allocationAvailable =
                    allocation.getAmount()
                            .subtract(previouslyRefunded);

            BigDecimal lineAmount =
                    MoneyRules.normalize(
                            line.amount()
                    );

            if (
                    lineAmount.compareTo(
                            allocationAvailable
                    ) > 0
            ) {

                throw new FinanceConflictException(
                        "A devolução excede o valor disponível da alocação"
                );
            }

            totalReversal =
                    totalReversal.add(lineAmount);
        }

        // 28. Invariantes
        if (
                totalReversal.compareTo(
                        refundAmount
                ) > 0
        ) {

            throw new FinanceConflictException(
                    "As reversões excedem o valor total da devolução"
            );
        }

        if (
                totalReversal.compareTo(
                        requiredReversal
                ) < 0
        ) {

            throw new FinanceConflictException(
                    "Parte do valor já está alocada e precisa ser explicitamente revertida"
            );
        }

        // Calcular fingerprint
        String fingerprint =
                hashService.sha256(
                        String.join(
                                "|",
                                paymentId.toString(),
                                refundAmount.toPlainString(),
                                Objects.toString(dto.reason(), "")
                        )
                );

        // 29. Criar Refund
        Instant now = clock.instant();

        Refund refund =
                Refund.builder()

                        .id(UUID.randomUUID())

                        .payment(payment)

                        .amount(refundAmount)

                        .currency(
                                payment.getCurrency()
                        )

                        .reason(dto.reason())

                        .status(RefundStatus.PENDING)

                        .idempotencyKey(
                                idempotencyKey
                        )

                        .requestFingerprint(
                                fingerprint
                        )

                        .requestedAt(now)

                        .createdAt(now)

                        .updatedAt(now)

                        .version(0L)

                        .build();

        Refund saved =
                refundRepository.saveAndFlush(refund);

        // 30. Criar RefundAllocations
        for (int i = 0; i < lines.size(); i++) {

            RefundAllocationRequestDTO line =
                    lines.get(i);

            PaymentAllocation allocation =
                    allocationRepository
                            .findByIdForUpdate(
                                    line
                                            .paymentAllocationId()
                            )
                            .orElseThrow();

            BigDecimal lineAmount =
                    MoneyRules.normalize(
                            line.amount()
                    );

            RefundAllocation refundAllocation =
                    RefundAllocation.builder()

                            .id(UUID.randomUUID())

                            .refund(saved)

                            .paymentAllocation(
                                    allocation
                            )

                            .amount(lineAmount)

                            .createdAt(now)

                            .build();

            refundAllocationRepository.save(
                    refundAllocation
            );
        }

        refundAllocationRepository.flush();

        // 32. Auditoria de solicitação
        auditService.recordCriticalWrite(

                new AuditCommand(

                        actor.userId(),

                        actor.sessionId(),

                        AuditAction.REFUND_REQUESTED,

                        "REFUND",

                        saved.getId().toString(),

                        payment.getPatient().getId(),

                        payment.getClinic() != null
                                ? payment.getClinic().getId()
                                : null,

                        AuditOutcome.SUCCESS,

                        actor.correlationId(),

                        actor.sourceIp(),

                        actor.userAgentHash(),

                        Map.of(
                                "paymentId",
                                payment.getId().toString(),

                                "amount",
                                refundAmount.toPlainString(),

                                "reason",
                                dto.reason().name()
                        )
                )
        );

        log.info(
                "Refund criado: id={}, payment={}, amount={}, allocations={}, status=PENDING",
                saved.getId(),
                paymentId,
                refundAmount,
                lines.size()
        );

        return saved;
    }

    /**
     * 33-37. Confirma um reembolso
     * 
     * Transição: PENDING → CONFIRMED
     * Locks ordenados: Payment → Refund → PaymentAllocations → Receivables
     * 
     * @param refundId ID do reembolso
     * @param actor usuário que está confirmando
     * @return refund confirmado
     */
    @Transactional
    public Refund confirm(
            UUID refundId,
            SecurityActor actor
    ) {

        Instant now = clock.instant();

        // 34. Encontrar Payment primeiro
        UUID paymentId =
                refundRepository
                        .findPaymentId(refundId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Devolução não encontrada"
                                        )
                        );

        // 34. Lock Payment
        Payment payment =
                paymentRepository
                        .findByIdForUpdate(paymentId)
                        .orElseThrow();

        // Lock Refund
        Refund refund =
                refundRepository
                        .findByIdForUpdate(refundId)
                        .orElseThrow();

        // 35. Validar transição e confirmar
        refundStateMachine
                .validateTransition(
                        refund.getStatus(),
                        RefundStatus.CONFIRMED
                );

        refund.confirm(now);

        refundRepository.saveAndFlush(refund);

        // 36. Atualizar status do Payment
        BigDecimal totalRefunded =
                refundRepository
                        .sumConfirmedRefunds(paymentId);

        if (
                totalRefunded.compareTo(
                        payment.getAmount()
                ) == 0
        ) {

            payment.markRefunded(now);

        } else {

            payment.markPartiallyRefunded(now);
        }

        paymentRepository.saveAndFlush(payment);

        // 37. Recalcular status de Receivables
        List<RefundAllocation> reversals =
                refundAllocationRepository
                        .findByRefundId(refundId);

        List<UUID> receivableIds =
                reversals.stream()

                        .map(
                                r ->
                                        r.getPaymentAllocation()
                                                .getReceivable()
                                                .getId()
                        )

                        .distinct()

                        .sorted()

                        .toList();

        // Lock receivables na ordem
        for (UUID receivableId : receivableIds) {

            Receivable receivable =
                    receivableRepository
                            .findByIdForUpdate(
                                    receivableId
                            )
                            .orElseThrow();

            BigDecimal effectivePaid =
                    balanceService
                            .allocatedAmount(
                                    receivableId
                            );

            synchronizeReceivableStatus(
                    receivable,
                    effectivePaid
            );

            receivableRepository.save(
                    receivable
            );
        }

        // Auditoria de confirmação
        auditService.recordCriticalWrite(

                new AuditCommand(

                        actor.userId(),

                        actor.sessionId(),

                        AuditAction.REFUND_CONFIRMED,

                        "REFUND",

                        refund.getId().toString(),

                        payment.getPatient().getId(),

                        payment.getClinic() != null
                                ? payment.getClinic().getId()
                                : null,

                        AuditOutcome.SUCCESS,

                        actor.correlationId(),

                        actor.sourceIp(),

                        actor.userAgentHash(),

                        Map.of(
                                "paymentId",
                                payment.getId().toString(),

                                "amount",
                                refund.getAmount().toPlainString(),

                                "paymentStatus",
                                payment.getStatus().name()
                        )
                )
        );

        log.info(
                "Refund confirmado: id={}, payment={}, amount={}, newPaymentStatus={}",
                refund.getId(),
                paymentId,
                refund.getAmount(),
                payment.getStatus()
        );

        return refund;
    }

    /**
     * Sincroniza status de receivable baseado em saldo efetivo pago
     */
    private void synchronizeReceivableStatus(
            Receivable receivable,
            BigDecimal effectivePaid
    ) {

        BigDecimal netAmount = receivable.getNetAmount();

        if (
                effectivePaid.compareTo(
                        BigDecimal.ZERO
                ) == 0
        ) {

            // Nenhum pagamento efetivo
            receivable.markOpen();

        } else if (
                effectivePaid.compareTo(netAmount) >= 0
        ) {

            // Totalmente pago
            receivable.markPaid();

        } else {

            // Parcialmente pago
            receivable.markPartiallyPaid();
        }
    }

    /**
     * Normaliza e valida idempotency-key
     */
    private String normalizeIdempotencyKey(String value) {

        if (
                value == null
                ||
                value.isBlank()
                ||
                value.length() > 100
        ) {

            throw new FinanceValidationException(
                    "Idempotency-Key inválida"
            );
        }

        return value.trim();
    }

    /**
     * 43. Marca reembolso como falho
     * 
     * Transição: PENDING → FAILED
     * Não altera Payment/Receivable porque dinheiro não saiu
     * 
     * @param refundId ID do reembolso
     * @param actor usuário que está marcando como falho
     * @return refund marcado como falho
     */
    @Transactional
    public Refund fail(
            UUID refundId,
            SecurityActor actor
    ) {

        Instant now = clock.instant();

        Refund refund =
                refundRepository
                        .findByIdForUpdate(refundId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Devolução não encontrada"
                                        )
                        );

        // 43. Validar transição
        refundStateMachine
                .validateTransition(
                        refund.getStatus(),
                        RefundStatus.FAILED
                );

        // Marcar como falho
        refund.fail(now);

        Refund saved =
                refundRepository.saveAndFlush(refund);

        // Buscar payment para auditoria
        Payment payment =
                paymentRepository
                        .findById(refund.getPayment().getId())
                        .orElseThrow();

        // 42. Auditoria de falha
        auditService.recordCriticalWrite(

                new AuditCommand(

                        actor.userId(),

                        actor.sessionId(),

                        AuditAction.REFUND_FAILED,

                        "REFUND",

                        refund.getId().toString(),

                        payment.getPatient().getId(),

                        payment.getClinic() != null
                                ? payment.getClinic().getId()
                                : null,

                        AuditOutcome.SUCCESS,

                        actor.correlationId(),

                        actor.sourceIp(),

                        actor.userAgentHash(),

                        Map.of(
                                "paymentId",
                                payment.getId().toString(),

                                "amount",
                                refund.getAmount().toPlainString()
                        )
                )
        );

        log.info(
                "Refund marcado como falho: id={}, payment={}",
                refund.getId(),
                payment.getId()
        );

        return saved;
    }

    /**
     * 44. Cancela reembolso pendente
     * 
     * Transição: PENDING → CANCELLED
     * Não altera Payment/Receivable porque operação foi cancelada
     * 
     * @param refundId ID do reembolso
     * @param actor usuário que está cancelando
     * @return refund cancelado
     */
    @Transactional
    public Refund cancel(
            UUID refundId,
            SecurityActor actor
    ) {

        Instant now = clock.instant();

        Refund refund =
                refundRepository
                        .findByIdForUpdate(refundId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Devolução não encontrada"
                                        )
                        );

        // 44. Validar transição
        refundStateMachine
                .validateTransition(
                        refund.getStatus(),
                        RefundStatus.CANCELLED
                );

        // Marcar como cancelado
        refund.cancel(now);

        Refund saved =
                refundRepository.saveAndFlush(refund);

        // Buscar payment para auditoria
        Payment payment =
                paymentRepository
                        .findById(refund.getPayment().getId())
                        .orElseThrow();

        // 42. Auditoria de cancelamento
        auditService.recordCriticalWrite(

                new AuditCommand(

                        actor.userId(),

                        actor.sessionId(),

                        AuditAction.REFUND_CANCELLED,

                        "REFUND",

                        refund.getId().toString(),

                        payment.getPatient().getId(),

                        payment.getClinic() != null
                                ? payment.getClinic().getId()
                                : null,

                        AuditOutcome.SUCCESS,

                        actor.correlationId(),

                        actor.sourceIp(),

                        actor.userAgentHash(),

                        Map.of(
                                "paymentId",
                                payment.getId().toString(),

                                "amount",
                                refund.getAmount().toPlainString()
                        )
                )
        );

        log.info(
                "Refund cancelado: id={}, payment={}",
                refund.getId(),
                payment.getId()
        );

        return saved;
    }

    /**
     * Confirma reembolso recebido do provider
     * 
     * Chamado pelo webhook processor após verificação de assinatura
     * 
     * Transição: PENDING → CONFIRMED
     * Ou: já confirmado → retorna (webhook duplicado)
     * 
     * @param providerRefundId ID do refund no provider
     * @param provider tipo de provider
     * @param occurredAt quando ocorreu no provider
     */
    @Transactional
    public void confirmFromProvider(
            String providerRefundId,
            PaymentProviderType provider,
            Instant occurredAt
    ) {

        // Buscar refund com lock pessimista
        Refund refund =
                refundRepository
                        .findByProviderAndProviderRefundIdForUpdate(
                                provider.name(),
                                providerRefundId
                        )
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Reembolso externo não encontrado"
                                        )
                        );

        // Webhook repetido = operação idempotente
        if (refund.getStatus() == RefundStatus.CONFIRMED) {

            log.info(
                    "Reembolso já confirmado (webhook duplicado): " +
                            "provider={}, providerRefundId={}, status={}",
                    provider,
                    providerRefundId,
                    refund.getStatus()
            );

            return;
        }

        // Validar transição de estado
        refundStateMachine.validateTransition(
                refund.getStatus(),
                RefundStatus.CONFIRMED
        );

        // Confirmar reembolso
        refund.confirm(occurredAt);

        refundRepository.saveAndFlush(refund);

        // Buscar Payment para logging
        Payment payment =
                paymentRepository
                        .findById(refund.getPayment().getId())
                        .orElseThrow();

        log.info(
                "Reembolso confirmado do provider: " +
                        "id={}, provider={}, providerRefundId={}, amount={}, payment={}, occurredAt={}",
                refund.getId(),
                provider,
                providerRefundId,
                refund.getAmount(),
                payment.getId(),
                occurredAt
        );
    }

    /**
     * Marca reembolso como falho recebido do provider
     * 
     * Chamado pelo webhook processor quando provider informa falha no refund
     * 
     * Transição: PENDING → FAILED
     * Ou: já em estado terminal → retorna (webhook duplicado/obsoleto)
     * 
     * @param providerRefundId ID do refund no provider
     * @param provider tipo de provider
     * @param occurredAt quando ocorreu no provider
     */
    @Transactional
    public void failFromProvider(
            String providerRefundId,
            PaymentProviderType provider,
            Instant occurredAt
    ) {

        // Buscar refund com lock pessimista
        Refund refund =
                refundRepository
                        .findByProviderAndProviderRefundIdForUpdate(
                                provider.name(),
                                providerRefundId
                        )
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Reembolso externo não encontrado"
                                        )
                        );

        // Webhook repetido ou estado terminal
        if (
                refund.getStatus() == RefundStatus.FAILED
                ||
                refund.getStatus() == RefundStatus.CANCELLED
                ||
                refund.getStatus() == RefundStatus.CONFIRMED
        ) {

            log.info(
                    "Reembolso já em estado terminal (webhook descartado): " +
                            "provider={}, providerRefundId={}, status={}",
                    provider,
                    providerRefundId,
                    refund.getStatus()
            );

            return;
        }

        // Validar transição de estado
        refundStateMachine.validateTransition(
                refund.getStatus(),
                RefundStatus.FAILED
        );

        // Marcar como falho
        refund.fail(occurredAt);

        refundRepository.saveAndFlush(refund);

        // Buscar Payment para logging
        Payment payment =
                paymentRepository
                        .findById(refund.getPayment().getId())
                        .orElseThrow();

        log.info(
                "Reembolso marcado como falho do provider: " +
                        "id={}, provider={}, providerRefundId={}, amount={}, payment={}, occurredAt={}",
                refund.getId(),
                provider,
                providerRefundId,
                refund.getAmount(),
                payment.getId(),
                occurredAt
        );
    }
}