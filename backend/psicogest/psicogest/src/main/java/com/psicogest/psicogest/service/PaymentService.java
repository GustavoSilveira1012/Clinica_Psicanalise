package com.psicogest.psicogest.service;

import com.psicogest.psicogest.domain.finance.MoneyRules;
import com.psicogest.psicogest.dto.PaymentCreateDTO;
import com.psicogest.psicogest.dto.PaymentResponseDTO;
import com.psicogest.psicogest.exception.FinanceValidationException;
import com.psicogest.psicogest.exception.IdempotencyConflictException;
import com.psicogest.psicogest.exception.ResourceNotFoundException;
import com.psicogest.psicogest.infrastructure.payment.provider.PaymentProviderType;
import com.psicogest.psicogest.model.entity.Clinic;
import com.psicogest.psicogest.model.entity.Patient;
import com.psicogest.psicogest.model.entity.Payment;
import com.psicogest.psicogest.model.entity.Payment.PaymentStatus;
import com.psicogest.psicogest.repository.ClinicRepository;
import com.psicogest.psicogest.repository.PatientRepository;
import com.psicogest.psicogest.repository.PaymentAllocationRepository;
import com.psicogest.psicogest.repository.PaymentRepository;
import com.psicogest.psicogest.security.SecurityActor;
import com.psicogest.psicogest.security.SecurityHashService;
import com.psicogest.psicogest.security.audit.AuditAction;
import com.psicogest.psicogest.security.audit.AuditCommand;
import com.psicogest.psicogest.security.audit.AuditOutcome;
import com.psicogest.psicogest.security.audit.AuditService;
import com.psicogest.psicogest.domain.finance.PaymentStateMachine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Service para operações com pagamentos
 * 
 * 8. Criação com idempotency-key
 * 12. Fingerprint criptográfico
 * 13. Criação idempotente
 * 15. Criação completa com patient/clinic
 * 16. AuditLog na criação
 * 17. Confirmação manual
 */
@Slf4j
@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PatientRepository patientRepository;
    private final ClinicRepository clinicRepository;
    private final PaymentAllocationRepository allocationRepository;
    private final SecurityHashService hashService;
    private final AuditService auditService;
    private final PaymentStateMachine paymentStateMachine;

    public PaymentService(
            PaymentRepository paymentRepository,
            PatientRepository patientRepository,
            ClinicRepository clinicRepository,
            PaymentAllocationRepository allocationRepository,
            SecurityHashService hashService,
            AuditService auditService,
            PaymentStateMachine paymentStateMachine
    ) {
        this.paymentRepository = paymentRepository;
        this.patientRepository = patientRepository;
        this.clinicRepository = clinicRepository;
        this.allocationRepository = allocationRepository;
        this.hashService = hashService;
        this.auditService = auditService;
        this.paymentStateMachine = paymentStateMachine;
    }

    /**
     * 8, 13, 15. Cria novo pagamento com idempotency-key de forma idempotente
     * 
     * POST /payments
     * Header: Idempotency-Key
     * 
     * Lógica:
     * 1. Normaliza e valida idempotency-key
     * 2. Calcula fingerprint criptográfico
     * 3. Busca por idempotency-key existente
     * 4. Se existe e fingerprint bate: retorna existente (idempotência)
     * 5. Se existe e fingerprint diferente: erro (conflito)
     * 6. Se não existe: busca patient, resolveFinancialClinic, cria novo
     * 
     * @param idempotencyKey chave de idempotência do header
     * @param dto dados do pagamento
     * @param actor usuário/contexto de segurança
     * @return pagamento criado ou existente
     */
    public PaymentResponseDTO create(
            String idempotencyKey,
            PaymentCreateDTO dto,
            SecurityActor actor
    ) {

        // 9. Validar idempotency-key
        String validatedKey =
                normalizeIdempotencyKey(idempotencyKey);

        // 12. Calcular fingerprint
        String fingerprint =
                createFingerprint(dto);

        // 13. Buscar por idempotency-key existente
        Optional<Payment> existing =
                paymentRepository
                        .findByIdempotencyKey(
                                validatedKey
                        );

        if (existing.isPresent()) {

            Payment payment = existing.get();

            // 13. Validar que fingerprint bate
            if (
                    !MessageDigest.isEqual(

                            payment
                                    .getRequestFingerprint()
                                    .getBytes(
                                            StandardCharsets.US_ASCII
                                    ),

                            fingerprint
                                    .getBytes(
                                            StandardCharsets.US_ASCII
                                    )
                    )
            ) {

                throw new IdempotencyConflictException(
                        "A chave de idempotência já foi utilizada para outra operação"
                );
            }

            log.info(
                    "Retornando pagamento existente (idempotência): id={}, key={}",
                    payment.getId(),
                    validatedKey
            );

            return toResponseDTO(payment);
        }

        // 15. Buscar patient
        Patient patient =
                patientRepository
                        .findById(
                                dto.patientId()
                        )
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Paciente não encontrado"
                                        )
                        );

        // Normalizar amount
        BigDecimal amount =
                MoneyRules.normalize(dto.amount());

        Instant now = Instant.now();

        // 15. Resolver clínica financeira
        Clinic clinic =
                resolveFinancialClinic(
                        actor,
                        dto
                );

        // 15. Criar payment
        Payment payment =
                Payment.builder()

                        .id(UUID.randomUUID())

                        .patient(patient)

                        .clinic(clinic)

                        .amount(amount)

                        .currency("BRL")

                        .paymentMethod(dto.paymentMethod())

                        .status(PaymentStatus.PENDING)

                        .provider(
                                normalizeNullable(
                                        dto.provider()
                                )
                        )

                        .providerTransactionId(
                                normalizeNullable(
                                        dto.providerTransactionId()
                                )
                        )

                        .idempotencyKey(validatedKey)

                        .requestFingerprint(fingerprint)

                        .createdAt(now)

                        .updatedAt(now)

                        .version(0L)

                        .build();

        // Persistir
        Payment saved =
                paymentRepository.saveAndFlush(
                        payment
                );

        log.info(
                "Pagamento criado: id={}, amount={}, method={}, patient={}, key={}",
                saved.getId(),
                amount,
                dto.paymentMethod(),
                patient.getId(),
                validatedKey
        );

        // 16. Auditar criação
        auditService.recordCriticalWrite(

                new AuditCommand(

                        actor.userId(),

                        actor.sessionId(),

                        AuditAction.PAYMENT_CREATED,

                        "PAYMENT",

                        saved.getId()
                                .toString(),

                        saved.getPatient()
                                .getId(),

                        saved.getClinic() != null
                                ? saved.getClinic()
                                    .getId()
                                : null,

                        AuditOutcome.SUCCESS,

                        actor.correlationId(),

                        actor.sourceIp(),

                        actor.userAgentHash(),

                        Map.of(
                                "amount",
                                saved.getAmount()
                                        .toPlainString(),

                                "currency",
                                saved.getCurrency(),

                                "method",
                                saved.getPaymentMethod()
                                        .name()
                        )
                )
        );

        return toResponseDTO(saved);
    }

    /**
     * 17. Confirma pagamento manualmente
     * 
     * POST /payments/{paymentId}/confirm
     * 
     * Futuro: gateway webhook poderá chamar a mesma lógica
     * 
     * @param paymentId ID do pagamento
     * @param receivedAt quando foi recebido (ou now())
     * @param actor usuário que confirmou
     * @return pagamento confirmado
     */
    @Transactional
    public PaymentResponseDTO confirm(
            UUID paymentId,
            Instant receivedAt,
            SecurityActor actor
    ) {

        // Buscar com lock pessimista
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

        // Validar transição de estado
        paymentStateMachine
                .validateTransition(

                        payment.getStatus(),

                        PaymentStatus.CONFIRMED
                );

        // Confirmar via método da entity
        payment.confirm(
                receivedAt != null
                        ? receivedAt
                        : Instant.now()
        );

        // Persistir
        Payment saved =
                paymentRepository
                        .saveAndFlush(
                                payment
                        );

        // Auditar confirmação
        auditPaymentConfirmation(
                saved,
                actor
        );

        log.info(
                "Pagamento confirmado: id={}, patient={}, amount={}",
                saved.getId(),
                saved.getPatient().getId(),
                saved.getAmount()
        );

        return toResponseDTO(saved);
    }

    /**
     * 9. Normaliza e valida idempotency-key
     */
    private String normalizeIdempotencyKey(
            String value
    ) {

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
     * 12. Calcula fingerprint criptográfico
     */
    private String createFingerprint(
            PaymentCreateDTO dto
    ) {

        String canonical =
                String.join(
                        "|",

                        String.valueOf(
                                dto.patientId()
                        ),

                        MoneyRules
                                .normalize(
                                        dto.amount()
                                )
                                .toPlainString(),

                        dto.paymentMethod()
                                .name(),

                        Objects.toString(
                                dto.provider(),
                                ""
                        ),

                        Objects.toString(
                                dto.providerTransactionId(),
                                ""
                        )
                );

        return hashService.sha256(
                canonical
        );
    }

    /**
     * Normaliza valores nullable (trim e null se vazio)
     */
    private String normalizeNullable(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    /**
     * 15. Resolve clínica financeira do contexto
     * 
     * Futuro: extrair do SecurityActor ou request context
     * Por enquanto retorna null (opcional)
     */
    private Clinic resolveFinancialClinic(
            SecurityActor actor,
            PaymentCreateDTO dto
    ) {

        // Futuro: implementar lógica de resolução
        // Por enquanto retorna null
        return null;
    }

    /**
     * 16. Audita confirmação de pagamento
     */
    private void auditPaymentConfirmation(
            Payment payment,
            SecurityActor actor
    ) {

        auditService.recordCriticalWrite(

                new AuditCommand(

                        actor.userId(),

                        actor.sessionId(),

                        AuditAction.PAYMENT_CONFIRMED,

                        "PAYMENT",

                        payment.getId()
                                .toString(),

                        payment.getPatient()
                                .getId(),

                        payment.getClinic() != null
                                ? payment.getClinic()
                                    .getId()
                                : null,

                        AuditOutcome.SUCCESS,

                        actor.correlationId(),

                        actor.sourceIp(),

                        actor.userAgentHash(),

                        Map.of(
                                "amount",
                                payment.getAmount()
                                        .toPlainString(),

                                "currency",
                                payment.getCurrency(),

                                "receivedAt",
                                payment.getReceivedAt()
                                        .toString()
                        )
                )
        );
    }

    /**
     * Converte Payment para DTO de resposta com saldos
     */
    private PaymentResponseDTO toResponseDTO(
            Payment payment
    ) {

        // 39. Calcular saldos
        BigDecimal allocatedAmount =
                allocationRepository
                        .sumAllocatedByPayment(
                                payment.getId()
                        );

        allocatedAmount =
                MoneyRules.normalize(
                        allocatedAmount
                );

        BigDecimal availableAmount =
                payment.getAmount()
                        .subtract(
                                allocatedAmount
                        );

        availableAmount =
                MoneyRules.normalize(
                        availableAmount
                );

        return new PaymentResponseDTO(

                payment.getId(),

                payment.getPatient() != null
                        ? payment.getPatient().getId()
                        : null,

                payment.getClinic() != null
                        ? payment.getClinic().getId()
                        : null,

                payment.getAmount(),

                allocatedAmount,

                availableAmount,

                payment.getCurrency(),

                payment.getPaymentMethod(),

                payment.getStatus(),

                payment.getReceivedAt(),

                payment.getCreatedAt()
        );
    }

    /**
     * 35. Confirma pagamento recebido do provider
     * 
     * Chamado pelo webhook processor após verificação de assinatura
     * Implementa idempotência em 2 camadas:
     * 1. Inbox (UNIQUE provider + providerEventId)
     * 2. State Machine (transições legítimas)
     * 
     * Transição: PENDING → CONFIRMED
     * Ou: já confirmado → retorna (webhook duplicado)
     * 
     * @param provider tipo de provider
     * @param providerTransactionId ID da transação no provider
     * @param occurredAt quando ocorreu no provider
     * @throws ResourceNotFoundException se transação não encontrada
     * @throws InvalidFinanceTransitionException se transição inválida
     */
    @Transactional
    public void confirmFromProvider(
            PaymentProviderType provider,
            String providerTransactionId,
            Instant occurredAt
    ) {

        // 35. Buscar pagamento com lock pessimista
        Payment payment =
                paymentRepository
                        .findByProviderAndProviderTransactionIdForUpdate(
                                provider.name(),
                                providerTransactionId
                        )
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Transação financeira externa não encontrada"
                                        )
                        );

        // 35. Webhook repetido = operação idempotente
        if (
                payment.getStatus() == PaymentStatus.CONFIRMED
                ||
                payment.getStatus() == PaymentStatus.PARTIALLY_REFUNDED
                ||
                payment.getStatus() == PaymentStatus.REFUNDED
        ) {

            log.info(
                    "Pagamento já confirmado (webhook duplicado): " +
                            "provider={}, providerTransactionId={}, status={}",
                    provider,
                    providerTransactionId,
                    payment.getStatus()
            );

            return;
        }

        // 35. Validar transição de estado
        paymentStateMachine.validateTransition(
                payment.getStatus(),
                PaymentStatus.CONFIRMED
        );

        // 35. Confirmar pagamento
        payment.confirm(occurredAt);

        paymentRepository.saveAndFlush(payment);

        log.info(
                "Pagamento confirmado do provider: " +
                        "id={}, provider={}, providerTransactionId={}, amount={}, occurredAt={}",
                payment.getId(),
                provider,
                providerTransactionId,
                payment.getAmount(),
                occurredAt
        );
    }

    /**
     * Marca pagamento como falho recebido do provider
     * 
     * Chamado pelo webhook processor quando provider informa falha
     * 
     * Transição: PENDING → FAILED
     * Ou: já em estado terminal → retorna (webhook duplicado/obsoleto)
     * 
     * @param provider tipo de provider
     * @param providerTransactionId ID da transação no provider
     * @param occurredAt quando ocorreu no provider
     */
    @Transactional
    public void failFromProvider(
            PaymentProviderType provider,
            String providerTransactionId,
            Instant occurredAt
    ) {

        // Buscar pagamento com lock pessimista
        Payment payment =
                paymentRepository
                        .findByProviderAndProviderTransactionIdForUpdate(
                                provider.name(),
                                providerTransactionId
                        )
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Transação financeira externa não encontrada"
                                        )
                        );

        // Webhook repetido ou estado terminal
        if (
                payment.getStatus() == PaymentStatus.FAILED
                ||
                payment.getStatus() == PaymentStatus.CANCELLED
                ||
                payment.getStatus() == PaymentStatus.CONFIRMED
                ||
                payment.getStatus() == PaymentStatus.PARTIALLY_REFUNDED
                ||
                payment.getStatus() == PaymentStatus.REFUNDED
        ) {

            log.info(
                    "Pagamento já em estado terminal (webhook descartado): " +
                            "provider={}, providerTransactionId={}, status={}",
                    provider,
                    providerTransactionId,
                    payment.getStatus()
            );

            return;
        }

        // Validar transição de estado
        paymentStateMachine.validateTransition(
                payment.getStatus(),
                PaymentStatus.FAILED
        );

        // Marcar como falho
        payment.fail();

        paymentRepository.saveAndFlush(payment);

        log.info(
                "Pagamento marcado como falho do provider: " +
                        "id={}, provider={}, providerTransactionId={}, amount={}, occurredAt={}",
                payment.getId(),
                provider,
                providerTransactionId,
                payment.getAmount(),
                occurredAt
        );
    }
}
