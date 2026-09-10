package com.psicogest.psicogest.controller;

import com.psicogest.psicogest.dto.PaymentCreateDTO;
import com.psicogest.psicogest.dto.PaymentResponseDTO;
import com.psicogest.psicogest.security.SecurityActor;
import com.psicogest.psicogest.security.SecurityActorFactory;
import com.psicogest.psicogest.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * 8, 15, 17. Controller para operações de pagamento
 * 
 * POST /payments - cria novo pagamento (com idempotency-key)
 * POST /payments/{paymentId}/confirm - confirma manualmente
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final SecurityActorFactory securityActorFactory;

    public PaymentController(
            PaymentService paymentService,
            SecurityActorFactory securityActorFactory
    ) {
        this.paymentService = paymentService;
        this.securityActorFactory = securityActorFactory;
    }

    /**
     * 8, 15. Cria novo pagamento com idempotency-key
     * 
     * POST /api/v1/payments
     * 
     * Header:
     *   Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
     * 
     * Body:
     * {
     *   "patientId": 123,
     *   "amount": 300.00,
     *   "paymentMethod": "PIX",
     *   "provider": null,
     *   "providerTransactionId": null,
     *   "description": "Referência 001"
     * }
     * 
     * Response: 201 CREATED
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponseDTO create(
            @RequestHeader("Idempotency-Key")
            String idempotencyKey,

            @Valid
            @RequestBody
            PaymentCreateDTO dto,

            Authentication authentication,
            HttpServletRequest request
    ) {

        log.info(
                "POST /payments: idempotencyKey={}, amount={}, method={}, patient={}",
                idempotencyKey,
                dto.amount(),
                dto.paymentMethod(),
                dto.patientId()
        );

        SecurityActor actor =
                securityActorFactory.from(
                        authentication,
                        request
                );

        return paymentService.create(
                idempotencyKey,
                dto,
                actor
        );
    }

    /**
     * 17. Confirma pagamento manualmente
     * 
     * POST /api/v1/payments/{paymentId}/confirm
     * 
     * Body (opcional):
     * {
     *   "receivedAt": "2026-09-09T20:30:00Z"
     * }
     * 
     * Se receivedAt não for informado, usa now()
     * Futuro: webhook do gateway poderá chamar a mesma lógica
     */
    @PostMapping("/{paymentId}/confirm")
    @ResponseStatus(HttpStatus.OK)
    public PaymentResponseDTO confirm(
            @PathVariable
            UUID paymentId,

            @RequestParam(required = false)
            Instant receivedAt,

            Authentication authentication,
            HttpServletRequest request
    ) {

        log.info(
                "POST /payments/{}/confirm: receivedAt={}",
                paymentId,
                receivedAt
        );

        SecurityActor actor =
                securityActorFactory.from(
                        authentication,
                        request
                );

        return paymentService.confirm(
                paymentId,
                receivedAt,
                actor
        );
    }
}
