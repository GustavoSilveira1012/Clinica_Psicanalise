package com.psicogest.psicogest.service;

import com.psicogest.psicogest.domain.finance.FinanceAuditHelper;
import com.psicogest.psicogest.domain.finance.MoneyRules;
import com.psicogest.psicogest.domain.finance.ReceivableStateMachine;
import com.psicogest.psicogest.dto.PaymentAllocationCreateDTO;
import com.psicogest.psicogest.dto.PaymentAllocationResponseDTO;
import com.psicogest.psicogest.exception.FinanceConflictException;
import com.psicogest.psicogest.exception.FinanceValidationException;
import com.psicogest.psicogest.exception.ResourceNotFoundException;
import com.psicogest.psicogest.model.entity.Payment;
import com.psicogest.psicogest.model.entity.Payment.PaymentStatus;
import com.psicogest.psicogest.model.entity.PaymentAllocation;
import com.psicogest.psicogest.model.entity.Receivable;
import com.psicogest.psicogest.model.entity.Receivable.ReceivableStatus;
import com.psicogest.psicogest.model.entity.Refund.RefundStatus;
import com.psicogest.psicogest.model.enums.ReceivableOriginType;
import com.psicogest.psicogest.repository.PaymentAllocationRepository;
import com.psicogest.psicogest.repository.PaymentRepository;
import com.psicogest.psicogest.repository.ReceivableRepository;
import com.psicogest.psicogest.repository.RefundRepository;
import com.psicogest.psicogest.security.SecurityActor;
import com.psicogest.psicogest.security.audit.AuditAction;
import com.psicogest.psicogest.security.audit.AuditCommand;
import com.psicogest.psicogest.security.audit.AuditOutcome;
import com.psicogest.psicogest.security.audit.AuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Service para alocação de pagamentos em contas a receber
 * 
 * 19. PaymentAllocationCreateDTO
 * 20. Transação crítica com locks pessimistas
 * 21. Validações de estado de receivable
 * 22. Validação de paciente
 * 23. Validação de clínica
 * 24. Validação de valor
 */
@Slf4j
@Service
@Transactional
public class PaymentAllocationService {

    private SubscriptionEntitlementService subscriptionEntitlementService;

    private final PaymentAllocationRepository allocationRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final ReceivableRepository receivableRepository;
    private final FinanceBalanceService balanceService;
    private final ReceivableStateMachine receivableStateMachine;
    private final AuditService auditService;

    public PaymentAllocationService(
            PaymentAllocationRepository allocationRepository,
            PaymentRepository paymentRepository,
            RefundRepository refundRepository,
            ReceivableRepository receivableRepository,
            FinanceBalanceService balanceService,
            ReceivableStateMachine receivableStateMachine,
            AuditService auditService
    ) {
        this.allocationRepository = allocationRepository;
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.receivableRepository = receivableRepository;
        this.balanceService = balanceService;
        this.receivableStateMachine = receivableStateMachine;
        this.auditService = auditService;
    }

    @Autowired
    public void setSubscriptionEntitlementService(
            SubscriptionEntitlementService subscriptionEntitlementService
    ) {
        this.subscriptionEntitlementService = subscriptionEntitlementService;
    }

    /**
     * 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31. 
     * Aloca pagamento para uma conta a receber
     * 
     * POST /payments/{paymentId}/allocations
     * 
     * Transação crítica com ordem de locks:
     * 1. Lock Payment (sempre primeiro para evitar deadlock)
     * 2. Lock Receivable
     * 3. Validações de estado, paciente, clínica, valor
     * 4. Validar saldos (Payment e Receivable)
     * 5. Criar allocation
     * 6. Sincronizar status de Receivable
     * 7. Persistir Receivable
     * 8. Auditar
     * 
     * @param paymentId ID do pagamento
     * @param dto dados da alocação
     * @param actor usuário que fez a alocação
     * @return alocação criada
     */
    @Transactional
    public PaymentAllocationResponseDTO allocate(
            UUID paymentId,
            PaymentAllocationCreateDTO dto,
            SecurityActor actor
    ) {

        // 20. Lock Payment primeiro (ordem consistente)
        Payment payment =
                paymentRepository
                        .findByIdForUpdate(
                                paymentId
                        )
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Pagamento não encontrado"
                                        )
                        );

        // 20. Validar que payment está confirmado
        if (
                payment.getStatus()
                        != PaymentStatus.CONFIRMED

                &&

                payment.getStatus()
                        != PaymentStatus.PARTIALLY_REFUNDED
        ) {

            throw new FinanceConflictException(
                    "Pagamento não está disponível para alocação"
            );
        }

        // 40. Validar que não existe refund pendente
        if (
                refundRepository
                        .existsByPaymentIdAndStatus(
                                paymentId,
                                RefundStatus.PENDING
                        )
        ) {

            throw new FinanceConflictException(
                    "Pagamento possui devolução pendente"
            );
        }

        // 20. Lock Receivable (segundo na ordem)
        Receivable receivable =
                receivableRepository
                        .findByIdForUpdate(
                                dto.receivableId()
                        )
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Cobrança não encontrada"
                                        )
                        );

        // 21. Validar que receivable não está cancelada
        if (
                receivable.getStatus()
                        == ReceivableStatus.CANCELLED
        ) {

            throw new FinanceConflictException(
                    "Cobrança cancelada não pode receber pagamento"
            );
        }

        // 21. Validar que receivable não está totalmente paga
        if (
                receivable.getStatus()
                        == ReceivableStatus.PAID
        ) {

            throw new FinanceConflictException(
                    "Cobrança já está totalmente paga"
            );
        }

        // 22. Validar que pacientes batem
        if (
                !payment.getPatient()
                        .getId()
                        .equals(
                                receivable
                                        .getPatient()
                                        .getId()
                        )
        ) {

            throw new FinanceConflictException(
                    "Pagamento e cobrança pertencem a pacientes diferentes"
            );
        }

        // 23. Validar que clínicas batem
        if (
                !Objects.equals(
                        clinicId(payment),
                        clinicId(receivable)
                )
        ) {

            throw new FinanceConflictException(
                    "Pagamento e cobrança pertencem a contextos financeiros diferentes"
            );
        }

        // 24. Normalizar e validar valor
        BigDecimal requested =
                MoneyRules.normalize(
                        dto.amount()
                );

        if (
                requested.compareTo(
                        BigDecimal.ZERO
                ) <= 0
        ) {

            throw new FinanceValidationException(
                    "Valor da alocação deve ser maior que zero"
            );
        }

        // 25. Validar saldo disponível do pagamento
        // 41. Usar balanceService para cálculo oficial
        BigDecimal paymentAvailable =
                balanceService
                        .availablePaymentAmount(
                                payment
                        );

        if (
                requested.compareTo(
                        paymentAvailable
                ) > 0
        ) {

            throw new FinanceConflictException(
                    "O valor excede o saldo disponível do pagamento"
            );
        }

        // 26. Validar saldo disponível da cobrança
        BigDecimal receivableAllocated =
                allocationRepository
                        .sumEffectiveAllocation(
                                receivable.getId()
                        );

        BigDecimal outstanding =
                receivable
                        .getNetAmount()
                        .subtract(
                                receivableAllocated
                        );

        outstanding =
                MoneyRules.normalize(
                        outstanding
                );

        if (
                requested.compareTo(
                        outstanding
                ) > 0
        ) {

            throw new FinanceConflictException(
                    "O valor excede o saldo da cobrança"
            );
        }

        // 27. Criar allocation
        Instant now = Instant.now();

        PaymentAllocation allocation =
                PaymentAllocation.builder()

                        .id(UUID.randomUUID())

                        .payment(payment)

                        .receivable(receivable)

                        .amount(requested)

                        .createdAt(now)

                        .build();

        PaymentAllocation saved =
                allocationRepository.saveAndFlush(
                        allocation
                );

        log.info(
                "Alocação criada: pagamento={}, conta={}, valor={}",
                paymentId,
                dto.receivableId(),
                requested
        );

        // 28. Atualizar status da conta baseado em novo alocado
        BigDecimal newAllocated =
                receivableAllocated
                        .add(
                                requested
                        );

        synchronizeReceivableStatus(
                receivable,
                newAllocated
        );

        // 30. Persistir Receivable com novo status
        receivableRepository.saveAndFlush(
                receivable
        );

        if (subscriptionEntitlementService != null
                && receivable.getOriginType() == ReceivableOriginType.SUBSCRIPTION_CYCLE
                && receivable.getOriginId() != null) {
            SubscriptionEntitlementService.Trigger trigger =
                    receivable.getStatus() == ReceivableStatus.PAID
                            ? SubscriptionEntitlementService.Trigger.FULL_PAYMENT
                            : SubscriptionEntitlementService.Trigger.FIRST_PAYMENT;
            subscriptionEntitlementService.grantIfAllowed(receivable.getOriginId(), trigger);
        }

        // 31. Auditar alocação
        auditService.recordCriticalWrite(

                new AuditCommand(

                        actor.userId(),

                        actor.sessionId(),

                        AuditAction.PAYMENT_ALLOCATED,

                        "PAYMENT_ALLOCATION",

                        saved.getId()
                                .toString(),

                        payment.getPatient()
                                .getId(),

                        clinicId(payment),

                        AuditOutcome.SUCCESS,

                        actor.correlationId(),

                        actor.sourceIp(),

                        actor.userAgentHash(),

                        Map.of(
                                "paymentId",
                                payment.getId()
                                        .toString(),

                                "receivableId",
                                receivable.getId()
                                        .toString(),

                                "amount",
                                requested
                                        .toPlainString(),

                                "receivableStatus",
                                receivable
                                        .getStatus()
                                        .name()
                        )
                )
        );

        return toResponseDTO(saved);
    }

    /**
     * 28. Sincroniza status de Receivable baseado em alocação
     * 
     * Helper para 28. Atualiza status conforme saldo alocado
     */
    private void synchronizeReceivableStatus(
            Receivable receivable,
            BigDecimal allocated
    ) {

        // 29. Proteção defensiva contra saldo negativo
        if (
                allocated.compareTo(
                        receivable.getNetAmount()
                ) > 0
        ) {

            throw new IllegalStateException(
                    "Invariante financeira violada: cobrança recebeu valor acima do permitido"
            );
        }

        int comparison =
                allocated.compareTo(
                        receivable
                                .getNetAmount()
                );

        if (comparison == 0) {

            receivable.markPaid();

            return;
        }

        if (allocated.signum() > 0) {

            receivable.markPartiallyPaid();

            return;
        }

        receivable.markOpen();
    }

    /**
     * 23. Extrai ID da clínica de um pagamento (helper)
     */
    private Long clinicId(Payment payment) {

        return payment.getClinic() != null
                ? payment.getClinic().getId()
                : null;
    }

    /**
     * 23. Extrai ID da clínica de uma conta (helper)
     */
    private Long clinicId(Receivable receivable) {

        return receivable.getClinic() != null
                ? receivable.getClinic().getId()
                : null;
    }

    /**
     * Converte PaymentAllocation para DTO
     */
    private PaymentAllocationResponseDTO toResponseDTO(
            PaymentAllocation allocation
    ) {

        return new PaymentAllocationResponseDTO(

                allocation.getId(),

                allocation.getPayment().getId(),

                allocation.getReceivable().getId(),

                allocation.getAmount(),

                allocation.getCreatedAt()
        );
    }
}
