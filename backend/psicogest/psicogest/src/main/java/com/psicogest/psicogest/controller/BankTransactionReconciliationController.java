package com.psicogest.psicogest.controller;

import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.psicogest.psicogest.dto.IgnoreBankTransactionDTO;
import com.psicogest.psicogest.dto.ReconcilePaymentDTO;
import com.psicogest.psicogest.dto.ReconcileProviderSettlementDTO;
import com.psicogest.psicogest.dto.ReconciliationSuggestionDTO;
import com.psicogest.psicogest.model.entity.BankReconciliationAllocation;
import com.psicogest.psicogest.security.SecurityActor;
import com.psicogest.psicogest.security.SecurityActorFactory;
import com.psicogest.psicogest.service.bank.BankReconciliationService;

import lombok.extern.slf4j.Slf4j;

/**
 * Controller para reconciliação de lançamentos bancários
 * 
 * Endpoints:
 * - POST /bank-transactions/{id}/reconcile/payment → reconciliar com pagamento
 * - GET /bank-transactions/{id}/suggestions → listar sugestões
 * - POST /bank-transactions/{id}/ignore → ignorar lançamento
 */
@Slf4j
@RestController
@RequestMapping("/bank-transactions/{transactionId}")
public class BankTransactionReconciliationController {

    private final BankReconciliationService reconciliationService;

    private final SecurityActorFactory securityActorFactory;

    public BankTransactionReconciliationController(
            BankReconciliationService reconciliationService,
            SecurityActorFactory securityActorFactory
    ) {
        this.reconciliationService = reconciliationService;
        this.securityActorFactory = securityActorFactory;
    }

    /**
     * Reconcilia lançamento bancário com pagamento
     * 
     * POST /bank-transactions/{id}/reconcile/payment
     * 
     * @param transactionId ID do lançamento
     * @param dto dados de reconciliação
     * @param authentication contexto de segurança
     * @param request requisição HTTP
     * @return allocation criada
     */
    @PostMapping("/reconcile/payment")
    public ResponseEntity<BankReconciliationAllocation>
            reconcileWithPayment(
                    @PathVariable
                    UUID transactionId,

                    @Valid
                    @RequestBody
                    ReconcilePaymentDTO dto,

                    Authentication authentication,

                    HttpServletRequest request
            ) {

        SecurityActor actor =
                securityActorFactory
                        .from(authentication, request);

        log.info(
                "Reconciliando lançamento bancário com pagamento: " +
                        "transactionId={}, paymentId={}, amount={}",
                transactionId,
                dto.paymentId(),
                dto.amount()
        );

        BankReconciliationAllocation allocation =
                reconciliationService
                        .reconcileWithPayment(
                                transactionId,
                                dto,
                                actor
                        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(allocation);
    }

    /**
     * Lista sugestões de reconciliação
     * 
     * GET /bank-transactions/{id}/suggestions
     * 
     * @param transactionId ID do lançamento
     * @return lista de sugestões com confidence
     */
    @GetMapping("/suggestions")
    public ResponseEntity<List<ReconciliationSuggestionDTO>>
            suggestReconciliations(
                    @PathVariable
                    UUID transactionId
            ) {

        log.info(
                "Buscando sugestões de reconciliação: " +
                        "transactionId={}",
                transactionId
        );

        List<ReconciliationSuggestionDTO> suggestions =
                reconciliationService
                        .suggestReconciliations(
                                transactionId
                        );

        return ResponseEntity.ok(suggestions);
    }

    /**
     * Marca lançamento para ignorar
     * 
     * POST /bank-transactions/{id}/ignore
     * 
     * @param transactionId ID do lançamento
     * @param dto motivo da ignoração
     * @param authentication contexto de segurança
     * @param request requisição HTTP
     * @return 204 No Content
     */
    @PostMapping("/ignore")
    public ResponseEntity<Void> ignoreBankTransaction(
            @PathVariable
            UUID transactionId,

            @Valid
            @RequestBody
            IgnoreBankTransactionDTO dto,

            Authentication authentication,

            HttpServletRequest request
    ) {

        SecurityActor actor =
                securityActorFactory
                        .from(authentication, request);

        log.info(
                "Ignorando lançamento bancário: " +
                        "transactionId={}, reason={}",
                transactionId,
                dto.reason()
        );

        reconciliationService
                .ignoreBankTransaction(
                        transactionId,
                        dto,
                        actor
                );

        return ResponseEntity
                .noContent()
                .build();
    }

    /**
     * Reconcilia lançamento bancário com ProviderSettlement
     * 
     * POST /bank-transactions/{id}/reconcile/provider-settlement
     * 
     * @param transactionId ID do lançamento
     * @param dto dados de reconciliação (settlementId, amount)
     * @param authentication contexto de segurança
     * @param request requisição HTTP
     * @return allocation criada
     */
    @PostMapping("/reconcile/provider-settlement")
    public ResponseEntity<BankReconciliationAllocation>
            reconcileWithProviderSettlement(
                    @PathVariable
                    UUID transactionId,

                    @Valid
                    @RequestBody
                    ReconcileProviderSettlementDTO dto,

                    Authentication authentication,

                    HttpServletRequest request
            ) {

        SecurityActor actor =
                securityActorFactory
                        .from(authentication, request);

        log.info(
                "Reconciliando lançamento bancário com repasse: " +
                        "transactionId={}, settlementId={}, amount={}",
                transactionId,
                dto.settlementId(),
                dto.amount()
        );

        BankReconciliationAllocation allocation =
                reconciliationService
                        .reconcileWithProviderSettlement(
                                transactionId,
                                dto.settlementId(),
                                dto.amount(),
                                actor
                        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(allocation);
    }
}
