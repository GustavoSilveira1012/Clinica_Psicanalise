package com.psicogest.psicogest.controller;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.psicogest.psicogest.dto.CancelServiceInvoiceRequest;
import com.psicogest.psicogest.dto.FiscalOperationResponse;
import com.psicogest.psicogest.dto.ServiceInvoiceCreateRequest;
import com.psicogest.psicogest.dto.ServiceInvoiceResponse;
import com.psicogest.psicogest.dto.SubstituteServiceInvoiceRequest;
import com.psicogest.psicogest.security.SecurityActor;
import com.psicogest.psicogest.security.SecurityActorFactory;
import com.psicogest.psicogest.service.fiscal.ServiceInvoiceService;

import lombok.extern.slf4j.Slf4j;

/**
 * Endpoints de gerenciamento de notas fiscais de serviço
 */
@Slf4j
@RestController
@RequestMapping("/service-invoices")
public class ServiceInvoiceController {

    private final ServiceInvoiceService invoiceService;

    private final SecurityActorFactory securityActorFactory;

    public ServiceInvoiceController(
            ServiceInvoiceService invoiceService,
            SecurityActorFactory securityActorFactory
    ) {
        this.invoiceService = invoiceService;
        this.securityActorFactory = securityActorFactory;
    }

    /**
     * Criar nota fiscal em rascunho
     * POST /service-invoices
     */
    @PostMapping
    public ResponseEntity<ServiceInvoiceResponse> create(
            @Valid
            @RequestBody
            ServiceInvoiceCreateRequest request,

            Authentication authentication,

            HttpServletRequest httpRequest
    ) {

        SecurityActor actor =
                securityActorFactory
                        .from(authentication, httpRequest);

        log.info(
                "Criando nota fiscal: fiscalIssuerId={}",
                request.fiscalIssuerId()
        );

        ServiceInvoiceResponse response =
                invoiceService.createDraft(
                        request,
                        actor
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Atualizar nota fiscal em rascunho
     * PUT /service-invoices/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ServiceInvoiceResponse> update(
            @PathVariable
            UUID id,

            @Valid
            @RequestBody
            ServiceInvoiceCreateRequest request,

            Authentication authentication,

            HttpServletRequest httpRequest
    ) {

        SecurityActor actor =
                securityActorFactory
                        .from(authentication, httpRequest);

        log.info(
                "Atualizando nota fiscal: id={}",
                id
        );

        // TODO: implementar updateDraft
        return ResponseEntity.ok().build();
    }

    /**
     * Consultar nota fiscal
     * GET /service-invoices/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ServiceInvoiceResponse> getById(
            @PathVariable
            UUID id,

            Authentication authentication,

            HttpServletRequest httpRequest
    ) {

        SecurityActor actor =
                securityActorFactory
                        .from(authentication, httpRequest);

        log.info(
                "Consultando nota fiscal: id={}",
                id
        );

        // TODO: implementar getById
        return ResponseEntity.ok().build();
    }

    /**
     * Requisitar emissão
     * POST /service-invoices/{id}/issue
     */
    @PostMapping("/{id}/issue")
    public ResponseEntity<FiscalOperationResponse> requestIssue(
            @PathVariable
            UUID id,

            Authentication authentication,

            HttpServletRequest httpRequest
    ) {

        SecurityActor actor =
                securityActorFactory
                        .from(authentication, httpRequest);

        String idempotencyKey = UUID.randomUUID().toString();

        log.info(
                "Requisitando emissão: invoiceId={}",
                id
        );

        FiscalOperationResponse response =
                invoiceService.requestIssue(
                        id,
                        idempotencyKey,
                        actor
                );

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(response);
    }

    /**
     * Requisitar cancelamento
     * POST /service-invoices/{id}/cancel
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<FiscalOperationResponse> requestCancel(
            @PathVariable
            UUID id,

            @Valid
            @RequestBody
            CancelServiceInvoiceRequest request,

            Authentication authentication,

            HttpServletRequest httpRequest
    ) {

        SecurityActor actor =
                securityActorFactory
                        .from(authentication, httpRequest);

        log.info(
                "Requisitando cancelamento: invoiceId={}, reason={}",
                id,
                request.reasonCode()
        );

        // TODO: implementar requestCancel
        return ResponseEntity.ok().build();
    }

    /**
     * Requisitar substituição
     * POST /service-invoices/{id}/substitute
     */
    @PostMapping("/{id}/substitute")
    public ResponseEntity<FiscalOperationResponse> requestSubstitute(
            @PathVariable
            UUID id,

            @Valid
            @RequestBody
            SubstituteServiceInvoiceRequest request,

            Authentication authentication,

            HttpServletRequest httpRequest
    ) {

        SecurityActor actor =
                securityActorFactory
                        .from(authentication, httpRequest);

        log.info(
                "Requisitando substituição: originalId={}, newId={}",
                id,
                request.newInvoiceId()
        );

        // TODO: implementar requestSubstitute
        return ResponseEntity.ok().build();
    }
}
