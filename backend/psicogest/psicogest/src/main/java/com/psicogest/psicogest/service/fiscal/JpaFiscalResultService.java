package com.psicogest.psicogest.service.fiscal;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.psicogest.psicogest.model.entity.FiscalOperation;
import com.psicogest.psicogest.model.entity.ServiceInvoice;
import com.psicogest.psicogest.model.enums.FiscalOperationStatus;
import com.psicogest.psicogest.model.enums.FiscalOperationType;
import com.psicogest.psicogest.model.enums.ServiceInvoiceStatus;
import com.psicogest.psicogest.repository.FiscalOperationRepository;
import com.psicogest.psicogest.security.audit.AuditAction;
import com.psicogest.psicogest.security.audit.AuditCommand;
import com.psicogest.psicogest.security.audit.AuditOutcome;
import com.psicogest.psicogest.security.audit.AuditService;

@Service
public class JpaFiscalResultService implements FiscalResultService {

    private final FiscalOperationRepository repository;
    private final AuditService auditService;
    private final Clock clock;

    public JpaFiscalResultService(FiscalOperationRepository repository, AuditService auditService, Clock clock) {
        this.repository = repository;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void apply(UUID operationId, FiscalIssueResult result) {
        FiscalOperation operation = repository.findByIdForUpdate(operationId)
                .orElseThrow(() -> new IllegalStateException("Operação fiscal não encontrada"));
        ServiceInvoice invoice = operation.getInvoice();
        Instant now = clock.instant();
        operation.setCompletedAt(now);
        operation.setStatus(result.success() ? FiscalOperationStatus.COMPLETED : FiscalOperationStatus.FAILED);
        operation.setErrorMessage(result.success() ? null : limit(result.errorMessage()));
        if (result.success()) {
            invoice.setNfseId(result.nfseId());
            if (operation.getOperationType() == FiscalOperationType.ISSUE) {
                invoice.markAuthorized(now);
            } else if (operation.getOperationType() == FiscalOperationType.CANCEL) {
                invoice.markCancelled(now);
            } else {
                invoice.markReconciliationRequired(now);
            }
        } else if (invoice.getStatus() == ServiceInvoiceStatus.PROCESSING) {
            invoice.markRejected(now, limit(result.errorMessage()));
        } else {
            invoice.markError(now, limit(result.errorMessage()));
        }
        repository.save(operation);
        AuditAction action = action(operation.getOperationType(), result.success());
        auditService.recordCriticalWrite(new AuditCommand(null, null, action, "SERVICE_INVOICE",
                invoice.getId().toString(), null, invoice.getClinic().getId(),
                result.success() ? AuditOutcome.SUCCESS : AuditOutcome.FAILURE, null, null, null,
                Map.of("operationId", operationId.toString(), "status", operation.getStatus().name())));
    }

    private AuditAction action(FiscalOperationType type, boolean success) {
        return switch (type) {
            case ISSUE -> success ? AuditAction.SERVICE_INVOICE_AUTHORIZED : AuditAction.SERVICE_INVOICE_REJECTED;
            case CANCEL -> success ? AuditAction.SERVICE_INVOICE_CANCELLED : AuditAction.SERVICE_INVOICE_CANCEL_REQUESTED;
            case SUBSTITUTE -> success ? AuditAction.SERVICE_INVOICE_SUBSTITUTED : AuditAction.SERVICE_INVOICE_SUBSTITUTION_REQUESTED;
        };
    }

    private String limit(String value) { return value == null ? "Falha não detalhada pelo provedor" : value.substring(0, Math.min(500, value.length())); }
}
