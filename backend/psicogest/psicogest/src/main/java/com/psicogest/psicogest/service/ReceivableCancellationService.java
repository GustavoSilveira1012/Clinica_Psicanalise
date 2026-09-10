package com.psicogest.psicogest.service;

import com.psicogest.psicogest.domain.finance.MoneyRules;
import com.psicogest.psicogest.dto.ApplyCreditDTO;
import com.psicogest.psicogest.dto.ReceivableCancellationDTO;
import com.psicogest.psicogest.dto.ReceivableCancellationResponseDTO;
import com.psicogest.psicogest.dto.ReceivableResponseDTO;
import com.psicogest.psicogest.exception.FinanceConflictException;
import com.psicogest.psicogest.exception.FinanceValidationException;
import com.psicogest.psicogest.exception.ResourceNotFoundException;
import com.psicogest.psicogest.model.entity.CreditAccount;
import com.psicogest.psicogest.model.entity.CreditEntry;
import com.psicogest.psicogest.model.entity.PaymentAllocation;
import com.psicogest.psicogest.model.entity.Receivable;
import com.psicogest.psicogest.model.entity.ReceivableCancellation;
import com.psicogest.psicogest.model.entity.ReceivableCancellation.ReceivableCancellationStatus;
import com.psicogest.psicogest.model.entity.User;
import com.psicogest.psicogest.model.enums.CreditEntryDirection;
import com.psicogest.psicogest.model.enums.CreditEntryType;
import com.psicogest.psicogest.model.enums.ReceivableCancellationMode;
import com.psicogest.psicogest.repository.CreditEntryRepository;
import com.psicogest.psicogest.repository.PaymentAllocationRepository;
import com.psicogest.psicogest.repository.ReceivableCancellationRepository;
import com.psicogest.psicogest.repository.ReceivableRepository;
import com.psicogest.psicogest.security.SecurityActor;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service para cancelamento de cobranças
 * 
 * 21. Cancelamento com crédito ou refund
 * 36. Aplicação de crédito
 */
@Slf4j
@Service
@Transactional
public class ReceivableCancellationService {

    private final ReceivableRepository receivableRepository;
    private final ReceivableCancellationRepository cancellationRepository;
    private final PaymentAllocationRepository allocationRepository;
    private final CreditEntryRepository creditEntryRepository;
    private final CreditAccountService creditAccountService;
    private final FinanceBalanceService balanceService;
    private final RefundService refundService;
    private final AuditService auditService;
    private final Clock clock;

    public ReceivableCancellationService(
            ReceivableRepository receivableRepository,
            ReceivableCancellationRepository cancellationRepository,
            PaymentAllocationRepository allocationRepository,
            CreditEntryRepository creditEntryRepository,
            CreditAccountService creditAccountService,
            FinanceBalanceService balanceService,
            RefundService refundService,
            AuditService auditService,
            Clock clock
    ) {
        this.receivableRepository = receivableRepository;
        this.cancellationRepository = cancellationRepository;
        this.allocationRepository = allocationRepository;
        this.creditEntryRepository = creditEntryRepository;
        this.creditAccountService = creditAccountService;
        this.balanceService = balanceService;
        this.refundService = refundService;
        this.auditService = auditService;
        this.clock = clock;
    }

    /**
     * 22-28. Cancela uma cobrança
     * 
     * Fluxo:
     * - Se nada foi pago: CANCELLED simples
     * - Se foi pago + mode = CREDIT_BALANCE: cria crédito
     * - Se foi pago + mode = REFUND: inicia refunds
     * 
     * @param receivableId ID da cobrança
     * @param dto dados do cancelamento
     * @param actor usuário que cancela
     * @return cancelamento realizado
     */
    @Transactional
    public ReceivableCancellationResponseDTO cancel(
            UUID receivableId,
            ReceivableCancellationDTO dto,
            SecurityActor actor
    ) {

        Instant now = clock.instant();

        // Lock receivable
        Receivable receivable =
                receivableRepository
                        .findByIdForUpdate(receivableId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Cobrança não encontrada"
                                        )
                        );

        // Validar estado
        if (
                receivable.getStatus()
                        == Receivable.ReceivableStatus.CANCELLED

                ||

                receivable.getStatus()
                        == Receivable.ReceivableStatus.CANCELLATION_PENDING
        ) {

            throw new FinanceConflictException(
                    "Cobrança não pode ser cancelada neste estado"
            );
        }

        // 22. Calcular quanto já foi pago
        BigDecimal paid =
                balanceService
                        .allocatedAmount(receivableId);

        // 23. Se nada foi pago
        if (paid.signum() == 0) {

            return cancelWithoutSettlement(
                    receivable,
                    dto.reason(),
                    actor,
                    now
            );
        }

        // 24. Mode obrigatório se foi pago
        if (
                dto.mode() == null
                ||
                dto.mode()
                        == ReceivableCancellationMode.NONE
        ) {

            throw new FinanceValidationException(
                    "Informe se o valor pago será reembolsado ou convertido em saldo credor"
            );
        }

        // 24. Rotear por mode
        return switch (dto.mode()) {

            case CREDIT_BALANCE ->
                    cancelWithCredit(
                            receivable,
                            dto.reason(),
                            actor,
                            now
                    );

            case REFUND ->
                    cancelWithRefund(
                            receivable,
                            dto.reason(),
                            actor,
                            now
                    );

            case NONE ->
                    throw new IllegalStateException();
        };
    }

    /**
     * 23. Cancelamento sem pagamento
     */
    private ReceivableCancellationResponseDTO cancelWithoutSettlement(
            Receivable receivable,
            com.psicogest.psicogest.model.enums.ReceivableCancellationReason reason,
            SecurityActor actor,
            Instant now
    ) {

        // Criar cancelamento
        ReceivableCancellation cancellation =
                ReceivableCancellation.builder()

                        .id(UUID.randomUUID())

                        .receivable(receivable)

                        .reason(reason)

                        .mode(ReceivableCancellationMode.NONE)

                        .status(
                                ReceivableCancellationStatus.COMPLETED
                        )

                        .createdBy(null) // TODO: obter de actor

                        .createdAt(now)

                        .completedAt(now)

                        .updatedAt(now)

                        .build();

        ReceivableCancellation saved =
                cancellationRepository.saveAndFlush(
                        cancellation
                );

        // Cancelar receivable
        receivable.cancel(now);
        receivableRepository.saveAndFlush(receivable);

        // Auditoria
        auditService.recordCriticalWrite(

                new AuditCommand(

                        actor.userId(),

                        actor.sessionId(),

                        AuditAction.RECEIVABLE_CANCELLED,

                        "RECEIVABLE",

                        receivable.getId().toString(),

                        receivable.getPatient().getId(),

                        clinicId(receivable),

                        AuditOutcome.SUCCESS,

                        actor.correlationId(),

                        actor.sourceIp(),

                        actor.userAgentHash(),

                        Map.of(
                                "mode",
                                "NONE",

                                "reason",
                                reason.name()
                        )
                )
        );

        return toResponseDTO(saved);
    }

    /**
     * 18-28. Cancelamento com saldo credor
     */
    private ReceivableCancellationResponseDTO cancelWithCredit(
            Receivable receivable,
            com.psicogest.psicogest.model.enums.ReceivableCancellationReason reason,
            SecurityActor actor,
            Instant now
    ) {

        // 25. Criar ReceivableCancellation
        ReceivableCancellation cancellation =
                ReceivableCancellation.builder()

                        .id(UUID.randomUUID())

                        .receivable(receivable)

                        .reason(reason)

                        .mode(ReceivableCancellationMode.CREDIT_BALANCE)

                        .status(
                                ReceivableCancellationStatus.COMPLETED
                        )

                        .createdBy(null) // TODO: obter de actor

                        .createdAt(now)

                        .completedAt(now)

                        .updatedAt(now)

                        .build();

        ReceivableCancellation saved =
                cancellationRepository.saveAndFlush(
                        cancellation
                );

        // Calcular valor pago
        BigDecimal paidAmount =
                balanceService
                        .allocatedAmount(receivable.getId());

        // 25. Buscar ou criar CreditAccount
        CreditAccount account =
                creditAccountService.getOrCreate(
                        receivable.getPatient(),
                        receivable.getClinic(),
                        receivable.getCurrency()
                );

        // 26. Criar CreditEntries para cada allocation
        createCancellationCredits(
                receivable,
                saved,
                account,
                now
        );

        // 27. Verificar invariante
        BigDecimal remainingPaid =
                balanceService
                        .allocatedAmount(
                                receivable.getId()
                        );

        if (remainingPaid.signum() != 0) {

            throw new IllegalStateException(
                    "Não foi possível liquidar financeiramente a cobrança cancelada"
            );
        }

        // 28. Cancelar receivable
        receivable.cancel(now);
        receivableRepository.saveAndFlush(receivable);

        // Auditoria
        auditService.recordCriticalWrite(

                new AuditCommand(

                        actor.userId(),

                        actor.sessionId(),

                        AuditAction.RECEIVABLE_CANCELLED,

                        "RECEIVABLE",

                        receivable.getId().toString(),

                        receivable.getPatient().getId(),

                        clinicId(receivable),

                        AuditOutcome.SUCCESS,

                        actor.correlationId(),

                        actor.sourceIp(),

                        actor.userAgentHash(),

                        Map.of(
                                "mode",
                                "CREDIT_BALANCE",

                                "creditAmount",
                                paidAmount.toPlainString()
                        )
                )
        );

        log.info(
                "Cobrança cancelada com crédito: receivable={}, amount={}",
                receivable.getId(),
                paidAmount
        );

        return toResponseDTO(saved);
    }

    /**
     * 30-33. Cancelamento com refund
     * (stub - lógica completa para próximas tarefas)
     */
    private ReceivableCancellationResponseDTO cancelWithRefund(
            Receivable receivable,
            com.psicogest.psicogest.model.enums.ReceivableCancellationReason reason,
            SecurityActor actor,
            Instant now
    ) {

        // Criar cancelamento PENDING
        ReceivableCancellation cancellation =
                ReceivableCancellation.builder()

                        .id(UUID.randomUUID())

                        .receivable(receivable)

                        .reason(reason)

                        .mode(ReceivableCancellationMode.REFUND)

                        .status(
                                ReceivableCancellationStatus.PENDING
                        )

                        .createdBy(null)

                        .createdAt(now)

                        .updatedAt(now)

                        .build();

        ReceivableCancellation saved =
                cancellationRepository.saveAndFlush(
                        cancellation
                );

        // Marcar receivable CANCELLATION_PENDING
        receivable.markCancellationPending();
        receivableRepository.saveAndFlush(receivable);

        log.info(
                "Cancelamento com refund iniciado: receivable={}, cancellation={}",
                receivable.getId(),
                saved.getId()
        );

        return toResponseDTO(saved);
    }

    /**
     * 26. Cria CreditEntries para cada allocation
     */
    private void createCancellationCredits(
            Receivable receivable,
            ReceivableCancellation cancellation,
            CreditAccount account,
            Instant now
    ) {

        // 19. Localizar cada allocation
        List<PaymentAllocation> allocations =
                allocationRepository
                        .findByReceivableIdOrdered(
                                receivable.getId()
                        );

        for (PaymentAllocation allocation : allocations) {

            // 20. Valor efetivamente aplicado
            BigDecimal amount =
                    balanceService
                            .effectiveAllocationAmount(
                                    allocation
                            );

            if (amount.signum() <= 0) {
                continue;
            }

            // 26. Criar CREDIT entry
            CreditEntry entry =
                    CreditEntry.builder()

                            .id(UUID.randomUUID())

                            .creditAccount(account)

                            .direction(
                                    CreditEntryDirection.CREDIT
                            )

                            .entryType(
                                    CreditEntryType
                                            .RECEIVABLE_CANCELLATION
                            )

                            .amount(amount)

                            .sourceReceivable(
                                    receivable
                            )

                            .sourcePaymentAllocation(
                                    allocation
                            )

                            .cancellation(
                                    cancellation
                            )

                            .createdBy(null)

                            .createdAt(now)

                            .build();

            creditEntryRepository.save(entry);
        }

        creditEntryRepository.flush();
    }

    /**
     * 36-40. Aplica crédito em uma cobrança
     * 
     * POST /receivables/{receivableId}/apply-credit
     * 
     * @param receivableId ID da cobrança
     * @param dto dados da aplicação
     * @param actor usuário que aplica
     * @return cobrança atualizada
     */
    @Transactional
    public ReceivableResponseDTO applyCredit(
            UUID receivableId,
            ApplyCreditDTO dto,
            SecurityActor actor
    ) {

        Instant now = clock.instant();

        // Buscar metadata do receivable
        Receivable metadata =
                receivableRepository
                        .findById(receivableId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Cobrança não encontrada"
                                        )
                        );

        // 37. Buscar CreditAccount
        CreditAccount account =
                creditAccountService.getOrCreate(
                        metadata.getPatient(),
                        metadata.getClinic(),
                        "BRL"
                );

        // Lock receivable
        Receivable receivable =
                receivableRepository
                        .findByIdForUpdate(receivableId)
                        .orElseThrow();

        // 38. Validar contextos
        // (já garantido por getOrCreate)

        // 39. Calcular saldos
        BigDecimal creditBalance =
                creditEntryRepository
                        .calculateBalance(
                                account.getId()
                        );

        BigDecimal outstanding =
                balanceService
                        .outstandingAmount(receivable);

        BigDecimal requested =
                MoneyRules.normalize(dto.amount());

        // 39. Validar crédito disponível
        if (
                requested.compareTo(creditBalance) > 0
        ) {

            throw new FinanceConflictException(
                    "Saldo credor insuficiente"
            );
        }

        // 39. Validar não excede cobrança
        if (
                requested.compareTo(outstanding) > 0
        ) {

            throw new FinanceConflictException(
                    "Valor excede o saldo da cobrança"
            );
        }

        // 40. Criar DEBIT entry
        CreditEntry entry =
                CreditEntry.builder()

                        .id(UUID.randomUUID())

                        .creditAccount(account)

                        .direction(
                                CreditEntryDirection.DEBIT
                        )

                        .entryType(
                                CreditEntryType
                                        .RECEIVABLE_APPLICATION
                        )

                        .amount(requested)

                        .targetReceivable(
                                receivable
                        )

                        .createdBy(null)

                        .createdAt(now)

                        .build();

        creditEntryRepository.saveAndFlush(entry);

        // 40. Recalcular saldo do receivable
        BigDecimal paid =
                balanceService
                        .allocatedAmount(receivableId);

        synchronizeReceivableStatus(
                receivable,
                paid
        );

        receivableRepository.saveAndFlush(receivable);

        // Auditoria
        auditService.recordCriticalWrite(

                new AuditCommand(

                        actor.userId(),

                        actor.sessionId(),

                        AuditAction.CREDIT_APPLIED,

                        "CREDIT_ENTRY",

                        entry.getId().toString(),

                        receivable.getPatient().getId(),

                        clinicId(receivable),

                        AuditOutcome.SUCCESS,

                        actor.correlationId(),

                        actor.sourceIp(),

                        actor.userAgentHash(),

                        Map.of(
                                "receivableId",
                                receivableId.toString(),

                                "amount",
                                requested.toPlainString()
                        )
                )
        );

        log.info(
                "Crédito aplicado: receivable={}, amount={}",
                receivableId,
                requested
        );

        return toReceivableResponseDTO(receivable);
    }

    /**
     * Sincroniza status de receivable baseado em saldo pago
     */
    private void synchronizeReceivableStatus(
            Receivable receivable,
            BigDecimal effectivePaid
    ) {

        BigDecimal netAmount = receivable.getNetAmount();

        if (effectivePaid.signum() == 0) {

            receivable.markOpen();

        } else if (
                effectivePaid.compareTo(netAmount) >= 0
        ) {

            receivable.markPaid();

        } else {

            receivable.markPartiallyPaid();
        }
    }

    /**
     * Extrai clinic ID
     */
    private Long clinicId(Receivable receivable) {

        return receivable.getClinic() != null
                ? receivable.getClinic().getId()
                : null;
    }

    /**
     * Converte para DTO
     */
    private ReceivableCancellationResponseDTO toResponseDTO(
            ReceivableCancellation cancellation
    ) {

        return new ReceivableCancellationResponseDTO(

                cancellation.getId(),

                cancellation.getReceivable().getId(),

                cancellation.getReason(),

                cancellation.getMode(),

                cancellation.getStatus(),

                cancellation.getCreatedAt(),

                cancellation.getCompletedAt()

        );
    }

    /**
     * Converte receivable para DTO
     */
    private ReceivableResponseDTO toReceivableResponseDTO(
            Receivable receivable
    ) {

        UUID receivableId = receivable.getId();

        return new ReceivableResponseDTO(

                receivable.getId(),

                receivable.getPatient().getId(),

                clinicId(receivable),

                receivable.getAppointment() != null
                        ? receivable.getAppointment().getId()
                        : null,

                receivable.getDescription(),

                receivable.getGrossAmount(),

                receivable.getDiscountAmount(),

                receivable.getNetAmount(),

                balanceService.allocatedAmount(receivableId),

                balanceService.outstandingAmount(receivable),

                isOverdue(receivable),

                receivable.getStatus(),

                receivable.getDueDate(),

                receivable.getCreatedAt()

        );
    }

    /**
     * Verifica se é vencido
     */
    private boolean isOverdue(Receivable receivable) {

        var today = java.time.LocalDate.now();
        return receivable.getDueDate().isBefore(today)
                &&
                receivable.getStatus()
                        != Receivable.ReceivableStatus.PAID;
    }
}
