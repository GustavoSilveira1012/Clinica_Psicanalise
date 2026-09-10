package com.psicogest.psicogest.service.fiscal;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.psicogest.psicogest.exception.ResourceNotFoundException;
import com.psicogest.psicogest.model.entity.ServiceInvoice;
import com.psicogest.psicogest.model.enums.ServiceInvoiceStatus;
import com.psicogest.psicogest.repository.ServiceInvoiceRepository;
import com.psicogest.psicogest.security.audit.AuditAction;
import com.psicogest.psicogest.security.audit.AuditCommand;
import com.psicogest.psicogest.security.audit.AuditOutcome;
import com.psicogest.psicogest.security.audit.AuditService;

import lombok.extern.slf4j.Slf4j;

/**
 * Adapter para processar callbacks de provedores fiscais
 * 
 * Webhooks recebem eventos:
 * - AUTHORIZED: NFS-e aprovada
 * - REJECTED: NFS-e rejeitada
 * - CANCELLED: NFS-e cancelada
 * - ERROR: Erro no processamento
 */
@Slf4j
@Service
@Transactional
public class FiscalWebhookAdapter {

    private final ServiceInvoiceRepository invoiceRepository;

    private final AuditService auditService;

    private final Clock clock;

    public FiscalWebhookAdapter(
            ServiceInvoiceRepository invoiceRepository,
            AuditService auditService,
            Clock clock
    ) {
        this.invoiceRepository = invoiceRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    /**
     * Processa callback de autorização
     * PROCESSING → AUTHORIZED
     */
    public void handleAuthorized(
            UUID invoiceId,
            String nfseId,
            String accessKey
    ) {

        Instant now = clock.instant();

        ServiceInvoice invoice =
                invoiceRepository
                        .findById(invoiceId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Nota fiscal não encontrada"
                                        )
                        );

        // Validar status
        if (invoice.getStatus() !=
                ServiceInvoiceStatus.PROCESSING) {

            log.warn(
                    "Callback AUTHORIZED para invoice em status inválido: id={}, status={}",
                    invoiceId,
                    invoice.getStatus()
            );

            return;
        }

        // Atualizar
        invoice.setNfseId(nfseId);
        invoice.markAuthorized(now);

        invoiceRepository.save(invoice);

        // Auditoria
        auditService.recordCriticalWrite(

                new AuditCommand(

                        "SYSTEM",

                        null,

                        AuditAction
                                .SERVICE_INVOICE_AUTHORIZED,

                        "SERVICE_INVOICE",

                        invoiceId.toString(),

                        null,

                        invoice.getClinic().getId(),

                        AuditOutcome.SUCCESS,

                        null,

                        null,

                        null,

                        Map.of(
                                "nfseId", nfseId,
                                "accessKey", accessKey
                        )
                )
        );

        log.info(
                "NFS-e autorizada: id={}, nfseId={}",
                invoiceId,
                nfseId
        );
    }

    /**
     * Processa callback de rejeição
     * PROCESSING → REJECTED
     */
    public void handleRejected(
            UUID invoiceId,
            String rejectionReason
    ) {

        Instant now = clock.instant();

        ServiceInvoice invoice =
                invoiceRepository
                        .findById(invoiceId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Nota fiscal não encontrada"
                                        )
                        );

        // Validar status
        if (invoice.getStatus() !=
                ServiceInvoiceStatus.PROCESSING) {

            log.warn(
                    "Callback REJECTED para invoice em status inválido: id={}, status={}",
                    invoiceId,
                    invoice.getStatus()
            );

            return;
        }

        // Atualizar
        invoice.markRejected(now, rejectionReason);

        invoiceRepository.save(invoice);

        // Auditoria
        auditService.recordCriticalWrite(

                new AuditCommand(

                        "SYSTEM",

                        null,

                        AuditAction
                                .SERVICE_INVOICE_REJECTED,

                        "SERVICE_INVOICE",

                        invoiceId.toString(),

                        null,

                        invoice.getClinic().getId(),

                        AuditOutcome.FAILURE,

                        null,

                        null,

                        null,

                        Map.of(
                                "reason",
                                rejectionReason
                        )
                )
        );

        log.info(
                "NFS-e rejeitada: id={}, reason={}",
                invoiceId,
                rejectionReason
        );
    }

    /**
     * Processa callback de cancelamento
     * CANCEL_PENDING → CANCELLED
     */
    public void handleCancelled(
            UUID invoiceId
    ) {

        Instant now = clock.instant();

        ServiceInvoice invoice =
                invoiceRepository
                        .findById(invoiceId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Nota fiscal não encontrada"
                                        )
                        );

        // Validar status
        if (invoice.getStatus() !=
                ServiceInvoiceStatus.CANCEL_PENDING) {

            log.warn(
                    "Callback CANCELLED para invoice em status inválido: id={}, status={}",
                    invoiceId,
                    invoice.getStatus()
            );

            return;
        }

        // Atualizar
        invoice.markCancelled(now);

        invoiceRepository.save(invoice);

        // Auditoria
        auditService.recordCriticalWrite(

                new AuditCommand(

                        "SYSTEM",

                        null,

                        AuditAction
                                .SERVICE_INVOICE_CANCELLED,

                        "SERVICE_INVOICE",

                        invoiceId.toString(),

                        null,

                        invoice.getClinic().getId(),

                        AuditOutcome.SUCCESS,

                        null,

                        null,

                        null,

                        null
                )
        );

        log.info(
                "NFS-e cancelada: id={}",
                invoiceId
        );
    }
}
