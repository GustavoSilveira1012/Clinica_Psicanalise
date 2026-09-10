package com.psicogest.psicogest.controller;

import com.psicogest.psicogest.dto.PaymentAllocationCreateDTO;
import com.psicogest.psicogest.dto.PaymentAllocationResponseDTO;
import com.psicogest.psicogest.security.SecurityActor;
import com.psicogest.psicogest.security.SecurityActorFactory;
import com.psicogest.psicogest.service.PaymentAllocationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 19, 20, 21, 22, 23, 24. Controller para alocação de pagamentos
 * 
 * POST /payments/{paymentId}/allocations
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments/{paymentId}/allocations")
public class PaymentAllocationController {

    private final PaymentAllocationService paymentAllocationService;
    private final SecurityActorFactory securityActorFactory;

    public PaymentAllocationController(
            PaymentAllocationService paymentAllocationService,
            SecurityActorFactory securityActorFactory
    ) {
        this.paymentAllocationService = paymentAllocationService;
        this.securityActorFactory = securityActorFactory;
    }

    /**
     * 19, 20, 21, 22, 23, 24. Aloca pagamento para uma conta a receber
     * 
     * POST /api/v1/payments/{paymentId}/allocations
     * 
     * Body:
     * {
     *   "receivableId": "123e4567-e89b-12d3-a456-426614174000",
     *   "amount": 150.00
     * }
     * 
     * Validações:
     * - Payment está confirmado
     * - Receivable não está cancelada/paga
     * - Pacientes batem
     * - Clínicas batem
     * - Valor > 0
     * - Saldo não ultrapassa limite
     * 
     * Response: 201 CREATED
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentAllocationResponseDTO allocate(
            @PathVariable
            UUID paymentId,

            @Valid
            @RequestBody
            PaymentAllocationCreateDTO dto,

            Authentication authentication,
            HttpServletRequest request
    ) {

        log.info(
                "POST /payments/{}/allocations: receivableId={}, amount={}",
                paymentId,
                dto.receivableId(),
                dto.amount()
        );

        SecurityActor actor =
                securityActorFactory.from(
                        authentication,
                        request
                );

        return paymentAllocationService.allocate(
                paymentId,
                dto,
                actor
        );
    }
}
