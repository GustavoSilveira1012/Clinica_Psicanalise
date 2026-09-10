package com.psicogest.psicogest.controller;

import com.psicogest.psicogest.dto.RefundCreateDTO;
import com.psicogest.psicogest.dto.RefundResponseDTO;
import com.psicogest.psicogest.model.entity.Refund;
import com.psicogest.psicogest.security.SecurityActorFactory;
import com.psicogest.psicogest.service.RefundService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Endpoints para operações com reembolsos
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments/{paymentId}/refunds")
public class RefundController {

    private final RefundService refundService;
    private final SecurityActorFactory securityActorFactory;

    public RefundController(
            RefundService refundService,
            SecurityActorFactory securityActorFactory
    ) {
        this.refundService = refundService;
        this.securityActorFactory = securityActorFactory;
    }

    /**
     * POST /api/v1/payments/{paymentId}/refunds
     * 
     * Cria um novo reembolso para um pagamento
     * 
     * Header:
     * Idempotency-Key: <uuid>
     * 
     * @param paymentId ID do pagamento
     * @param idempotencyKey chave de idempotência
     * @param dto dados do reembolso
     * @param auth contexto de segurança
     * @param request requisição HTTP
     * @return refund criado
     */
    @PostMapping
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'FINANCE')"
    )
    public ResponseEntity<RefundResponseDTO> create(
            @PathVariable UUID paymentId,
            @RequestHeader(
                    name = "Idempotency-Key",
                    required = true
            ) String idempotencyKey,
            @Valid @RequestBody RefundCreateDTO dto,
            Authentication auth,
            HttpServletRequest request
    ) {

        log.info(
                "POST /refunds: paymentId={}, amount={}, reason={}",
                paymentId,
                dto.amount(),
                dto.reason()
        );

        var actor =
                securityActorFactory.from(
                        auth,
                        request
                );

        Refund refund =
                refundService.create(
                        paymentId,
                        idempotencyKey,
                        dto,
                        actor
                );

        RefundResponseDTO response =
                toResponseDTO(refund);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Converte Refund entity para DTO
     */
    private RefundResponseDTO toResponseDTO(
            Refund refund
    ) {

        return new RefundResponseDTO(

                refund.getId(),

                refund.getPayment().getId(),

                refund.getAmount(),

                refund.getCurrency(),

                refund.getReason(),

                refund.getStatus(),

                refund.getRequestedAt(),

                refund.getConfirmedAt(),

                refund.getFailedAt(),

                refund.getCancelledAt()

        );
    }

    /**
     * POST /api/v1/payments/{paymentId}/refunds/{refundId}/confirm
     * 
     * Confirma um reembolso (PENDING → CONFIRMED)
     * 
     * @param paymentId ID do pagamento
     * @param refundId ID do reembolso
     * @param auth contexto de segurança
     * @param request requisição HTTP
     * @return refund confirmado
     */
    @PostMapping("/{refundId}/confirm")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'FINANCE')"
    )
    public ResponseEntity<RefundResponseDTO> confirm(
            @PathVariable UUID paymentId,
            @PathVariable UUID refundId,
            Authentication auth,
            HttpServletRequest request
    ) {

        log.info(
                "POST /refunds/{}/confirm: paymentId={}",
                refundId,
                paymentId
        );

        var actor =
                securityActorFactory.from(
                        auth,
                        request
                );

        Refund refund =
                refundService.confirm(
                        refundId,
                        actor
                );

        RefundResponseDTO response =
                toResponseDTO(refund);

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/payments/{paymentId}/refunds/{refundId}/fail
     * 
     * Marca reembolso como falho (PENDING → FAILED)
     * Gateway rejeitou o refund
     * 
     * @param paymentId ID do pagamento
     * @param refundId ID do reembolso
     * @param auth contexto de segurança
     * @param request requisição HTTP
     * @return refund marcado como falho
     */
    @PostMapping("/{refundId}/fail")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'FINANCE')"
    )
    public ResponseEntity<RefundResponseDTO> fail(
            @PathVariable UUID paymentId,
            @PathVariable UUID refundId,
            Authentication auth,
            HttpServletRequest request
    ) {

        log.info(
                "POST /refunds/{}/fail: paymentId={}",
                refundId,
                paymentId
        );

        var actor =
                securityActorFactory.from(
                        auth,
                        request
                );

        Refund refund =
                refundService.fail(
                        refundId,
                        actor
                );

        RefundResponseDTO response =
                toResponseDTO(refund);

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/payments/{paymentId}/refunds/{refundId}/cancel
     * 
     * Cancela reembolso pendente (PENDING → CANCELLED)
     * 
     * @param paymentId ID do pagamento
     * @param refundId ID do reembolso
     * @param auth contexto de segurança
     * @param request requisição HTTP
     * @return refund cancelado
     */
    @PostMapping("/{refundId}/cancel")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'FINANCE')"
    )
    public ResponseEntity<RefundResponseDTO> cancel(
            @PathVariable UUID paymentId,
            @PathVariable UUID refundId,
            Authentication auth,
            HttpServletRequest request
    ) {

        log.info(
                "POST /refunds/{}/cancel: paymentId={}",
                refundId,
                paymentId
        );

        var actor =
                securityActorFactory.from(
                        auth,
                        request
                );

        Refund refund =
                refundService.cancel(
                        refundId,
                        actor
                );

        RefundResponseDTO response =
                toResponseDTO(refund);

        return ResponseEntity.ok(response);
    }
}
