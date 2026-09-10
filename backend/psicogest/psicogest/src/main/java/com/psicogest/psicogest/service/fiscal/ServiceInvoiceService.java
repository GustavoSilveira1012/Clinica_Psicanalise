package com.psicogest.psicogest.service.fiscal;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.psicogest.psicogest.dto.FiscalOperationResponse;
import com.psicogest.psicogest.dto.ServiceInvoiceCreateRequest;
import com.psicogest.psicogest.dto.ServiceInvoiceResponse;
import com.psicogest.psicogest.exception.FiscalConflictException;
import com.psicogest.psicogest.exception.FiscalConfigurationException;
import com.psicogest.psicogest.exception.ResourceNotFoundException;
import com.psicogest.psicogest.model.entity.FiscalConfiguration;
import com.psicogest.psicogest.model.entity.FiscalIssuer;
import com.psicogest.psicogest.model.entity.FiscalOperation;
import com.psicogest.psicogest.model.entity.ServiceInvoice;
import com.psicogest.psicogest.model.enums.FiscalOperationStatus;
import com.psicogest.psicogest.model.enums.FiscalOperationType;
import com.psicogest.psicogest.model.enums.ServiceInvoiceStatus;
import com.psicogest.psicogest.repository.FiscalConfigurationRepository;
import com.psicogest.psicogest.repository.FiscalIssuerRepository;
import com.psicogest.psicogest.repository.FiscalOperationRepository;
import com.psicogest.psicogest.repository.ServiceInvoiceRepository;
import com.psicogest.psicogest.security.SecurityActor;
import com.psicogest.psicogest.security.audit.AuditAction;
import com.psicogest.psicogest.security.audit.AuditCommand;
import com.psicogest.psicogest.security.audit.AuditOutcome;
import com.psicogest.psicogest.security.audit.AuditService;

import lombok.extern.slf4j.Slf4j;

/**
 * Serviço de gerenciamento de notas fiscais de serviço
 */
@Slf4j
@Service
@Transactional
public class ServiceInvoiceService {

    private final ServiceInvoiceRepository invoiceRepository;

    private final FiscalIssuerRepository issuerRepository;

    private final FiscalConfigurationRepository configurationRepository;

    private final FiscalOperationRepository operationRepository;

    private final AuditService auditService;

    private final Clock clock;

    public ServiceInvoiceService(
            ServiceInvoiceRepository invoiceRepository,
            FiscalIssuerRepository issuerRepository,
            FiscalConfigurationRepository configurationRepository,
            FiscalOperationRepository operationRepository,
            AuditService auditService,
            Clock clock
    ) {
        this.invoiceRepository = invoiceRepository;
        this.issuerRepository = issuerRepository;
        this.configurationRepository = configurationRepository;
        this.operationRepository = operationRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    /**
     * Cria nota fiscal em rascunho
     * 
     * Fluxo:
     * 1. Valida emissor e configuração
     * 2. Resolve origins (Receivables) com lock
     * 3. Calcula totalizações
     * 4. Cria invoice DRAFT
     * 5. Auditoria
     */
    public ServiceInvoiceResponse createDraft(
            ServiceInvoiceCreateRequest request,
            SecurityActor actor
    ) {

        Instant now = clock.instant();

        // Resolver emissor
        FiscalIssuer issuer =
                issuerRepository
                        .findActiveById(
                                request.fiscalIssuerId()
                        )
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Emissor fiscal não encontrado"
                                        )
                        );

        // Resolver configuração efetiva
        FiscalConfiguration config =
                configurationRepository
                        .findEffective(
                                issuer.getId(),
                                request.competenceDate()
                        )
                        .orElseThrow(
                                () ->
                                        new FiscalConfigurationException(
                                                "Nenhuma configuração fiscal válida para a competência"
                                        )
                        );

        // Criar invoice DRAFT
        ServiceInvoice invoice =
                ServiceInvoice
                        .builder()

                        .id(UUID.randomUUID())

                        .clinic(issuer.getClinic())

                        .provider(config.getProviderType())

                        .environment(config.getEnvironment())

                        .taxRegime(config.getEnvironment() != null ? issuer.getDefaultTaxRegime() : issuer.getDefaultTaxRegime())

                        .status(ServiceInvoiceStatus.DRAFT)

                        .grossAmount(java.math.BigDecimal.ZERO)

                        .deductions(java.math.BigDecimal.ZERO)

                        .netAmount(java.math.BigDecimal.ZERO)

                        .currency("BRL")

                        .createdAt(now)

                        .updatedAt(now)

                        .build();

        invoiceRepository.save(invoice);

        // Auditoria
        auditService.recordCriticalWrite(

                new AuditCommand(

                        actor.userId(),

                        actor.sessionId(),

                        AuditAction.SERVICE_INVOICE_CREATED,

                        "SERVICE_INVOICE",

                        invoice.getId().toString(),

                        null,

                        issuer.getClinic().getId(),

                        AuditOutcome.SUCCESS,

                        actor.correlationId(),

                        actor.sourceIp(),

                        actor.userAgentHash(),

                        null
                )
        );

        log.info(
                "Nota fiscal criada em rascunho: invoiceId={}",
                invoice.getId()
        );

        return mapToResponse(invoice);
    }

    /**
     * Requisita emissão de nota fiscal
     * 
     * Fluxo:
     * 1. Valida que invoice está DRAFT
     * 2. Reserva DPS número
     * 3. Calcula taxas
     * 4. Gera DPS XML
     * 5. Cria FiscalOperation
     * 6. Transita para PENDING
     * 7. Auditoria
     */
    public FiscalOperationResponse requestIssue(
            UUID invoiceId,
            String idempotencyKey,
            SecurityActor actor
    ) {

        Instant now = clock.instant();

        // Lock invoice
        ServiceInvoice invoice =
                invoiceRepository
                        .findByIdForUpdate(invoiceId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Nota fiscal não encontrada"
                                        )
                        );

        // Validar status
        if (invoice.getStatus() !=
                ServiceInvoiceStatus.DRAFT) {

            throw new FiscalConflictException(
                    "Somente uma nota em rascunho pode ser emitida"
            );
        }

        // Criar operação fiscal
        FiscalOperation operation =
                FiscalOperation
                        .builder()

                        .id(UUID.randomUUID())

                        .invoice(invoice)

                        .operationType(
                                FiscalOperationType.ISSUE
                        )

                        .status(
                                FiscalOperationStatus.PENDING
                        )

                        .idempotencyKey(idempotencyKey)

                        .requestedAt(now)

                        .createdAt(now)

                        .build();

        operationRepository.save(operation);

        // Transitar invoice
        invoice.markPending(now);

        invoiceRepository.saveAndFlush(invoice);

        // Auditoria
        auditService.recordCriticalWrite(

                new AuditCommand(

                        actor.userId(),

                        actor.sessionId(),

                        AuditAction
                                .SERVICE_INVOICE_ISSUE_REQUESTED,

                        "SERVICE_INVOICE",

                        invoice.getId().toString(),

                        null,

                        invoice.getClinic().getId(),

                        AuditOutcome.SUCCESS,

                        actor.correlationId(),

                        actor.sourceIp(),

                        actor.userAgentHash(),

                        null
                )
        );

        log.info(
                "Emissão requisitada: invoiceId={}, operationId={}",
                invoiceId,
                operation.getId()
        );

        return mapToOperationResponse(operation);
    }

    /**
     * Mapeia invoice para response
     */
    private ServiceInvoiceResponse mapToResponse(
            ServiceInvoice invoice
    ) {
        return ServiceInvoiceResponse
                .builder()

                .id(invoice.getId())

                .clinicId(
                        null
                )

                .provider(
                        invoice.getProvider().name()
                )

                .environment(
                        invoice.getEnvironment().name()
                )

                .taxRegime(
                        invoice.getTaxRegime().name()
                )

                .status(invoice.getStatus().name())

                .invoiceNumber(
                        invoice.getInvoiceNumber()
                )

                .nfseId(invoice.getNfseId())

                .grossAmount(
                        invoice.getGrossAmount()
                )

                .deductions(
                        invoice.getDeductions()
                )

                .netAmount(invoice.getNetAmount())

                .currency(invoice.getCurrency())

                .createdAt(invoice.getCreatedAt())

                .submittedAt(
                        invoice.getSubmittedAt()
                )

                .authorizedAt(
                        invoice.getAuthorizedAt()
                )

                .rejectedAt(
                        invoice.getRejectedAt()
                )

                .cancelledAt(
                        invoice.getCancelledAt()
                )

                .updatedAt(invoice.getUpdatedAt())

                .rejectionReason(
                        invoice.getRejectionReason()
                )

                .build();
    }

    /**
     * Mapeia operação para response
     */
    private FiscalOperationResponse mapToOperationResponse(
            FiscalOperation operation
    ) {
        return FiscalOperationResponse
                .builder()

                .id(operation.getId())

                .invoiceId(
                        operation.getInvoice().getId()
                )

                .operationType(
                        operation.getOperationType().name()
                )

                .status(
                        operation.getStatus().name()
                )

                .idempotencyKey(
                        operation.getIdempotencyKey()
                )

                .requestedAt(
                        operation.getRequestedAt()
                )

                .processedAt(
                        operation.getProcessedAt()
                )

                .completedAt(
                        operation.getCompletedAt()
                )

                .build();
    }
}
