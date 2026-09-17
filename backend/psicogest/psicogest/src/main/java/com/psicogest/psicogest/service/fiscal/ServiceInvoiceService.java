package com.psicogest.psicogest.service.fiscal;

import java.time.Clock;
import java.time.Instant;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.psicogest.psicogest.dto.FiscalOperationResponse;
import com.psicogest.psicogest.dto.CancelServiceInvoiceRequest;
import com.psicogest.psicogest.dto.InvoiceOriginRequest;
import com.psicogest.psicogest.dto.ServiceInvoiceCreateRequest;
import com.psicogest.psicogest.dto.ServiceInvoiceResponse;
import com.psicogest.psicogest.dto.SubstituteServiceInvoiceRequest;
import com.psicogest.psicogest.exception.FiscalConflictException;
import com.psicogest.psicogest.exception.FiscalConfigurationException;
import com.psicogest.psicogest.exception.ResourceNotFoundException;
import com.psicogest.psicogest.model.entity.FiscalConfiguration;
import com.psicogest.psicogest.model.entity.FiscalIssuer;
import com.psicogest.psicogest.model.entity.FiscalOperation;
import com.psicogest.psicogest.model.entity.ServiceInvoice;
import com.psicogest.psicogest.model.entity.ServiceInvoiceItem;
import com.psicogest.psicogest.model.entity.Receivable;
import com.psicogest.psicogest.model.enums.FiscalOperationStatus;
import com.psicogest.psicogest.model.enums.FiscalOperationType;
import com.psicogest.psicogest.model.enums.ServiceInvoiceStatus;
import com.psicogest.psicogest.repository.FiscalConfigurationRepository;
import com.psicogest.psicogest.repository.FiscalIssuerRepository;
import com.psicogest.psicogest.repository.FiscalOperationRepository;
import com.psicogest.psicogest.repository.ServiceInvoiceRepository;
import com.psicogest.psicogest.repository.ReceivableRepository;
import com.psicogest.psicogest.service.FinanceBalanceService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final ReceivableRepository receivableRepository;

    private final FinanceBalanceService financeBalanceService;

    private final DpsSequenceService dpsSequenceService;

    private final ObjectMapper objectMapper;

    public ServiceInvoiceService(
            ServiceInvoiceRepository invoiceRepository,
            FiscalIssuerRepository issuerRepository,
            FiscalConfigurationRepository configurationRepository,
            FiscalOperationRepository operationRepository,
            AuditService auditService,
            Clock clock,
            ReceivableRepository receivableRepository,
            FinanceBalanceService financeBalanceService,
            DpsSequenceService dpsSequenceService,
            ObjectMapper objectMapper
    ) {
        this.invoiceRepository = invoiceRepository;
        this.issuerRepository = issuerRepository;
        this.configurationRepository = configurationRepository;
        this.operationRepository = operationRepository;
        this.auditService = auditService;
        this.clock = clock;
        this.receivableRepository = receivableRepository;
        this.financeBalanceService = financeBalanceService;
        this.dpsSequenceService = dpsSequenceService;
        this.objectMapper = objectMapper;
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

        ServiceInvoice invoice = ServiceInvoice.builder()
                        .id(UUID.randomUUID())
                        .clinic(issuer.getClinic())
                        .fiscalIssuer(issuer)
                        .competenceDate(request.competenceDate())
                        .layoutVersion(config.getLayoutVersion() == null || config.getLayoutVersion().isBlank()
                                ? "2.03" : config.getLayoutVersion())
                        .provider(config.getProviderType())
                        .environment(config.getEnvironment())
                        .taxRegime(issuer.getDefaultTaxRegime())
                        .status(ServiceInvoiceStatus.DRAFT)
                        .grossAmount(BigDecimal.ZERO)
                        .deductions(BigDecimal.ZERO)
                        .netAmount(BigDecimal.ZERO)
                        .currency("BRL")
                        .serviceDescription(request.serviceDescription().trim())
                        .createdAt(now)
                        .updatedAt(now)
                        .build();

        populateOrigins(invoice, request.origins(), issuer.getClinic());

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

        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw new FiscalConflictException("Idempotency-Key é obrigatório para emissão");
        }
        var existing = operationRepository.findByIdempotencyKey(idempotencyKey.trim());
        if (existing.isPresent()) {
            if (!existing.get().getInvoice().getId().equals(invoiceId)
                    || existing.get().getOperationType() != FiscalOperationType.ISSUE) {
                throw new FiscalConflictException("Idempotency-Key já usada em outra operação fiscal");
            }
            return mapToOperationResponse(existing.get());
        }

        // Validar status
        if (invoice.getStatus() !=
                ServiceInvoiceStatus.DRAFT) {

            throw new FiscalConflictException(
                    "Somente uma nota em rascunho pode ser emitida"
            );
        }

        if (invoice.getDpsNumber() == null) {
            invoice.setDpsNumber(dpsSequenceService.next(invoice.getFiscalIssuer().getId()));
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

                        .idempotencyKey(idempotencyKey.trim())

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

    public ServiceInvoiceResponse updateDraft(UUID invoiceId, ServiceInvoiceCreateRequest request, SecurityActor actor) {
        Instant now = clock.instant();
        ServiceInvoice invoice = invoiceRepository.findByIdForUpdate(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Nota fiscal não encontrada"));
        if (invoice.getStatus() != ServiceInvoiceStatus.DRAFT) {
            throw new FiscalConflictException("Somente rascunhos podem ser alterados");
        }
        FiscalIssuer issuer = issuerRepository.findActiveById(request.fiscalIssuerId())
                .orElseThrow(() -> new ResourceNotFoundException("Emissor fiscal não encontrado"));
        FiscalConfiguration config = configurationRepository.findEffective(issuer.getId(), request.competenceDate())
                .orElseThrow(() -> new FiscalConfigurationException("Nenhuma configuração fiscal válida para a competência"));
        if (!invoice.getClinic().getId().equals(issuer.getClinic().getId())) {
            throw new FiscalConflictException("Emissor pertence a outra clínica");
        }
        invoice.setFiscalIssuer(issuer);
        invoice.setCompetenceDate(request.competenceDate());
        invoice.setLayoutVersion(config.getLayoutVersion() == null || config.getLayoutVersion().isBlank() ? "2.03" : config.getLayoutVersion());
        invoice.setServiceDescription(request.serviceDescription().trim());
        invoice.getItems().clear();
        populateOrigins(invoice, request.origins(), issuer.getClinic());
        invoice.setUpdatedAt(now);
        invoiceRepository.save(invoice);
        auditService.recordCriticalWrite(new AuditCommand(actor.userId(), actor.sessionId(), AuditAction.SERVICE_INVOICE_UPDATED,
                "SERVICE_INVOICE", invoiceId.toString(), null, invoice.getClinic().getId(), AuditOutcome.SUCCESS,
                actor.correlationId(), actor.sourceIp(), actor.userAgentHash(), Map.of("originCount", request.origins().size())));
        return mapToResponse(invoice);
    }

    @Transactional(readOnly = true)
    public ServiceInvoiceResponse getById(UUID invoiceId, SecurityActor actor) {
        ServiceInvoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Nota fiscal não encontrada"));
        auditService.recordSensitiveRead(new AuditCommand(actor.userId(), actor.sessionId(), AuditAction.SERVICE_INVOICE_READ,
                "SERVICE_INVOICE", invoiceId.toString(), null, invoice.getClinic().getId(), AuditOutcome.SUCCESS,
                actor.correlationId(), actor.sourceIp(), actor.userAgentHash(), Map.of()));
        return mapToResponse(invoice);
    }

    @Transactional(readOnly = true)
    public List<ServiceInvoiceResponse> list(SecurityActor actor) {
        return invoiceRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .map(this::mapToResponse)
                .toList();
    }

    public FiscalOperationResponse requestCancel(UUID invoiceId, CancelServiceInvoiceRequest request,
                                                   String idempotencyKey, SecurityActor actor) {
        ServiceInvoice invoice = invoiceRepository.findByIdForUpdate(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Nota fiscal não encontrada"));
        if (invoice.getStatus() != ServiceInvoiceStatus.AUTHORIZED || invoice.getNfseId() == null) {
            throw new FiscalConflictException("Somente notas autorizadas podem ser canceladas");
        }
        return createOperation(invoice, FiscalOperationType.CANCEL, idempotencyKey,
                Map.of("reasonCode", request.reasonCode().trim()), actor, now -> invoice.markCancelPending(now));
    }

    public FiscalOperationResponse requestSubstitute(UUID invoiceId, SubstituteServiceInvoiceRequest request,
                                                      String idempotencyKey, SecurityActor actor) {
        ServiceInvoice invoice = invoiceRepository.findByIdForUpdate(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Nota fiscal não encontrada"));
        ServiceInvoice replacement = invoiceRepository.findById(request.newInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Nota substituta não encontrada"));
        if (invoice.getStatus() != ServiceInvoiceStatus.AUTHORIZED || replacement.getStatus() != ServiceInvoiceStatus.DRAFT) {
            throw new FiscalConflictException("A nota original deve estar autorizada e a substituta em rascunho");
        }
        if (!invoice.getClinic().getId().equals(replacement.getClinic().getId())) {
            throw new FiscalConflictException("Notas de clínicas diferentes não podem ser substituídas");
        }
        return createOperation(invoice, FiscalOperationType.SUBSTITUTE, idempotencyKey,
                Map.of("newInvoiceId", request.newInvoiceId().toString(), "newDpsNumber", replacement.getDpsNumber() == null ? 0L : replacement.getDpsNumber()), actor, now -> { });
    }

    private FiscalOperationResponse createOperation(ServiceInvoice invoice, FiscalOperationType type, String idempotencyKey,
                                                      Map<String, Object> payload, SecurityActor actor,
                                                      java.util.function.Consumer<Instant> transition) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw new FiscalConflictException("Idempotency-Key é obrigatório para esta operação");
        }
        var existing = operationRepository.findByIdempotencyKey(idempotencyKey.trim());
        if (existing.isPresent()) {
            if (!existing.get().getInvoice().getId().equals(invoice.getId())
                    || existing.get().getOperationType() != type) {
                throw new FiscalConflictException("Idempotency-Key já usada em outra operação fiscal");
            }
            return mapToOperationResponse(existing.get());
        }
        Instant now = clock.instant();
        FiscalOperation operation = FiscalOperation.builder().id(UUID.randomUUID()).invoice(invoice).operationType(type)
                .status(FiscalOperationStatus.PENDING).idempotencyKey(idempotencyKey.trim()).requestedAt(now).createdAt(now)
                .requestPayload(writePayload(payload)).build();
        transition.accept(now);
        operationRepository.save(operation);
        AuditAction action = type == FiscalOperationType.CANCEL ? AuditAction.SERVICE_INVOICE_CANCEL_REQUESTED : AuditAction.SERVICE_INVOICE_SUBSTITUTION_REQUESTED;
        auditService.recordCriticalWrite(new AuditCommand(actor.userId(), actor.sessionId(), action, "SERVICE_INVOICE",
                invoice.getId().toString(), null, invoice.getClinic().getId(), AuditOutcome.SUCCESS,
                actor.correlationId(), actor.sourceIp(), actor.userAgentHash(), Map.of("operationId", operation.getId().toString())));
        return mapToOperationResponse(operation);
    }

    private String writePayload(Map<String, Object> payload) {
        try { return objectMapper.writeValueAsString(payload); }
        catch (Exception exception) { throw new FiscalConfigurationException("Não foi possível preparar a operação fiscal", exception); }
    }

    private void populateOrigins(ServiceInvoice invoice, List<InvoiceOriginRequest> origins, com.psicogest.psicogest.model.entity.Clinic clinic) {
        Set<UUID> ids = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (InvoiceOriginRequest origin : origins) {
            if (!ids.add(origin.receivableId())) throw new FiscalConflictException("Uma cobrança não pode ser repetida na mesma nota");
            Receivable receivable = receivableRepository.findByIdForUpdate(origin.receivableId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cobrança de origem não encontrada"));
            if (receivable.getClinic() == null || !clinic.getId().equals(receivable.getClinic().getId())) {
                throw new FiscalConflictException("A cobrança pertence a outra clínica");
            }
            BigDecimal amount = origin.amount().setScale(2, RoundingMode.HALF_EVEN);
            if (amount.compareTo(financeBalanceService.outstandingAmount(receivable)) > 0) {
                throw new FiscalConflictException("Valor da nota excede o saldo da cobrança");
            }
            total = total.add(amount);
            invoice.addItem(ServiceInvoiceItem.builder().id(UUID.randomUUID()).description(invoice.getServiceDescription())
                    .amount(amount).itemType(ServiceInvoiceItem.ItemType.SERVICE).receivable(receivable)
                    .createdAt(clock.instant()).build());
        }
        total = total.setScale(2, RoundingMode.HALF_EVEN);
        invoice.setGrossAmount(total);
        invoice.setDeductions(BigDecimal.ZERO.setScale(2));
        invoice.setNetAmount(total);
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

                .clinicId(invoice.getClinic().getId())

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
