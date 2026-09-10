package com.psicogest.psicogest.service;

import com.psicogest.psicogest.domain.finance.FinanceAuditHelper;
import com.psicogest.psicogest.domain.finance.ReceivableStateMachine;
import com.psicogest.psicogest.exception.FinanceConflictException;
import com.psicogest.psicogest.exception.FinanceValidationException;
import com.psicogest.psicogest.model.entity.Payment;
import com.psicogest.psicogest.model.entity.PaymentAllocation;
import com.psicogest.psicogest.model.entity.Receivable;
import com.psicogest.psicogest.model.entity.Receivable.ReceivableStatus;
import com.psicogest.psicogest.repository.PaymentAllocationRepository;
import com.psicogest.psicogest.repository.PaymentRepository;
import com.psicogest.psicogest.repository.ReceivableRepository;
import com.psicogest.psicogest.security.audit.AuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 26. Service para alocação de pagamentos em contas a receber
 * 
 * Aplica validações cruciais:
 * - Pagamento e cobrança devem ter mesmo paciente
 * - Pagamento e cobrança devem estar no mesmo contexto de clínica
 * - Valor alocado não pode ultrapassar saldo pendente
 * - Transições de estado válidas
 */
@Slf4j
@Service
@Transactional
public class PaymentAllocationService {

    private final PaymentAllocationRepository allocationRepository;
    private final PaymentRepository paymentRepository;
    private final ReceivableRepository receivableRepository;
    private final FinanceBalanceService balanceService;
    private final ReceivableStateMachine receivableStateMachine;
    private final AuditService auditService;

    public PaymentAllocationService(
            PaymentAllocationRepository allocationRepository,
            PaymentRepository paymentRepository,
            ReceivableRepository receivableRepository,
            FinanceBalanceService balanceService,
            ReceivableStateMachine receivableStateMachine,
            AuditService auditService
    ) {
        this.allocationRepository = allocationRepository;
        this.paymentRepository = paymentRepository;
        this.receivableRepository = receivableRepository;
        this.balanceService = balanceService;
        this.receivableStateMachine = receivableStateMachine;
        this.auditService = auditService;
    }

    /**
     * Aloca pagamento para uma conta a receber
     * 
     * Validações:
     * 1. Payment e Receivable: mesmo paciente
     * 2. Payment e Receivable: mesmo contexto de clínica
     * 3. Valor a alocar: não ultrapassa saldo
     * 4. Transição de estado válida
     * 
     * @param paymentId ID do pagamento
     * @param receivableId ID da conta a receber
     * @param allocationAmount valor a alocar
     * @return PaymentAllocation criada
     */
    public PaymentAllocation allocate(
            UUID paymentId,
            UUID receivableId,
            BigDecimal allocationAmount
    ) {

        Payment payment =
                paymentRepository
                        .findById(paymentId)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Pagamento não encontrado"
                                )
                        );

        Receivable receivable =
                receivableRepository
                        .findById(receivableId)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Conta a receber não encontrada"
                                )
                        );

        // 26. VALIDAÇÃO 1: Mesmo paciente
        if (!payment.getPatient()
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

        // 26. VALIDAÇÃO 2: Mesmo contexto de clínica
        if (!Objects.equals(
                clinicId(payment),
                clinicId(receivable)
        )) {

            throw new FinanceConflictException(
                    "Contextos financeiros incompatíveis"
            );
        }

        // 27. VALIDAÇÃO 3: Saldo não pode ser ultrapassado
        BigDecimal currentAllocated =
                balanceService.allocatedAmount(
                        receivableId
                );

        BigDecimal newTotal =
                currentAllocated.add(
                        allocationAmount
                );

        if (newTotal.compareTo(
                receivable.getNetAmount()
        ) > 0) {

            throw new FinanceValidationException(
                    String.format(
                            "Alocação de %.2f ultrapassaria saldo: %.2f total vs %.2f limite",
                            allocationAmount,
                            newTotal,
                            receivable.getNetAmount()
                    )
            );
        }

        // Criar alocação
        PaymentAllocation allocation =
                PaymentAllocation.builder()

                        .id(UUID.randomUUID())

                        .payment(payment)

                        .receivable(receivable)

                        .amount(allocationAmount)

                        .createdAt(Instant.now())

                        .build();

        allocationRepository.saveAndFlush(
                allocation
        );

        log.info(
                "Alocação criada: pagamento={}, conta={}, valor={}",
                paymentId,
                receivableId,
                allocationAmount
        );

        // Atualizar status da conta se totalmente paga
        updateReceivableStatus(receivable);

        // 31. Auditar alocação de pagamento
        auditService.recordCriticalWrite(
                FinanceAuditHelper.paymentAllocated(
                        null, // actorUserId será obtido do contexto de segurança
                        receivable.getPatient().getId(),
                        null, // clinicId será obtido quando Receivable tiver clinic
                        paymentId,
                        receivableId,
                        allocationAmount,
                        payment.getCurrency()
                )
        );

        return allocation;
    }

    /**
     * Atualiza status da conta baseado em saldo
     */
    private void updateReceivableStatus(
            Receivable receivable
    ) {

        BigDecimal outstanding =
                balanceService.outstandingAmount(
                        receivable
                );

        ReceivableStatus currentStatus =
                receivable.getStatus();

        ReceivableStatus newStatus;

        if (outstanding.compareTo(
                BigDecimal.ZERO
        ) == 0) {

            newStatus = ReceivableStatus.PAID;

        } else if (outstanding.compareTo(
                receivable.getNetAmount()
        ) < 0) {

            newStatus = ReceivableStatus.PARTIALLY_PAID;

        } else {

            // Sem mudança
            return;
        }

        // Validar transição
        receivableStateMachine.validateTransition(
                currentStatus,
                newStatus
        );

        // Atualizar
        receivable.setStatus(newStatus);

        receivable.setUpdatedAt(Instant.now());

        receivableRepository.saveAndFlush(
                receivable
        );

        log.info(
                "Status da conta atualizado: {} -> {}",
                currentStatus,
                newStatus
        );
    }

    /**
     * Extrai ID da clínica de um pagamento
     * 
     * Futuro: quando Payment tiver relacionamento explícito com Clinic
     */
    private UUID clinicId(Payment payment) {
        // Por enquanto, retorna null (sem clínica no Payment)
        // Futuro será expandido quando Payment tiver @ManyToOne Clinic
        return null;
    }

    /**
     * Extrai ID da clínica de uma conta
     * 
     * Futuro: quando Receivable tiver relacionamento explícito com Clinic
     */
    private UUID clinicId(Receivable receivable) {
        // Por enquanto, retorna null (sem clínica no Receivable)
        // Futuro será expandido quando Receivable tiver @ManyToOne Clinic
        return null;
    }
}
