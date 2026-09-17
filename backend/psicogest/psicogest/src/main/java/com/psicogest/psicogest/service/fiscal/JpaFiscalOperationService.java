package com.psicogest.psicogest.service.fiscal;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.psicogest.psicogest.exception.ResourceNotFoundException;
import com.psicogest.psicogest.model.entity.FiscalOperation;
import com.psicogest.psicogest.model.entity.ServiceInvoice;
import com.psicogest.psicogest.model.enums.FiscalOperationStatus;
import com.psicogest.psicogest.model.enums.FiscalOperationType;
import com.psicogest.psicogest.repository.FiscalOperationRepository;

@Service
public class JpaFiscalOperationService implements FiscalOperationService {

    private final FiscalOperationRepository repository;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public JpaFiscalOperationService(FiscalOperationRepository repository, Clock clock, ObjectMapper objectMapper) {
        this.repository = repository;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public FiscalOperationClaim claim(UUID operationId) {
        FiscalOperation operation = repository.findByIdForUpdate(operationId)
                .orElseThrow(() -> new ResourceNotFoundException("Operação fiscal não encontrada"));
        if (operation.getStatus() != FiscalOperationStatus.PENDING) {
            return null;
        }

        Instant now = clock.instant();
        operation.setStatus(FiscalOperationStatus.CLAIMED);
        operation.setProcessedAt(now);
        if (operation.getInvoice().getStatus() == com.psicogest.psicogest.model.enums.ServiceInvoiceStatus.PENDING
                && operation.getOperationType() == FiscalOperationType.ISSUE) {
            operation.getInvoice().markProcessing(now);
        }
        repository.saveAndFlush(operation);
        ServiceInvoice invoice = operation.getInvoice();
        if (operation.getOperationType() == FiscalOperationType.ISSUE && invoice.getDpsNumber() == null) {
            throw new IllegalStateException("Nota fiscal sem número DPS reservado");
        }
        FiscalIssueCommand issue = operation.getOperationType() == FiscalOperationType.ISSUE
                ? new FiscalIssueCommand(invoice.getFiscalIssuer().getId(), invoice.getId(),
                    invoice.getDpsNumber() == null ? 0L : invoice.getDpsNumber(), invoice.getLayoutVersion(),
                    invoice.getCompetenceDate(), invoice.getGrossAmount(), invoice.getDeductions(),
                    invoice.getNetAmount(), invoice.getServiceDescription())
                : null;
        JsonNode payload = readPayload(operation.getRequestPayload());
        FiscalCancellationCommand cancellation = operation.getOperationType() == FiscalOperationType.CANCEL
                ? new FiscalCancellationCommand(invoice.getId(), invoice.getNfseId(),
                    text(payload, "reasonCode"), "")
                : null;
        FiscalSubstitutionCommand substitution = operation.getOperationType() == FiscalOperationType.SUBSTITUTE
                ? new FiscalSubstitutionCommand(invoice.getId(), uuid(payload, "newInvoiceId"), invoice.getNfseId(),
                    number(payload, "newDpsNumber"))
                : null;
        return new FiscalOperationClaim(operation.getId(), invoice.getProvider(), operation.getOperationType(), issue,
                cancellation, substitution);
    }

    private JsonNode readPayload(String payload) {
        if (payload == null || payload.isBlank()) return objectMapper.createObjectNode();
        try { return objectMapper.readTree(payload); }
        catch (Exception ignored) { return objectMapper.createObjectNode(); }
    }

    private String text(JsonNode node, String name) {
        JsonNode value = node.get(name);
        return value == null ? "" : value.asText("");
    }

    private UUID uuid(JsonNode node, String name) {
        try { return UUID.fromString(text(node, name)); }
        catch (IllegalArgumentException exception) { throw new IllegalStateException("Payload fiscal inválido"); }
    }

    private long number(JsonNode node, String name) {
        JsonNode value = node.get(name);
        if (value == null || !value.canConvertToLong()) throw new IllegalStateException("Payload fiscal inválido");
        return value.longValue();
    }
}
