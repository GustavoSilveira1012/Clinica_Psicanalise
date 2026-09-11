package com.psicogest.psicogest.controller;

import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.psicogest.psicogest.dto.PatientPackageResponseDTO;
import com.psicogest.psicogest.model.entity.SessionCreditEntry;
import com.psicogest.psicogest.security.SecurityActor;
import com.psicogest.psicogest.security.SecurityActorFactory;
import com.psicogest.psicogest.service.PackageConsumptionDomainService;
import com.psicogest.psicogest.service.SessionCreditService;

import lombok.extern.slf4j.Slf4j;

/**
 * Controller para gerenciamento de pacotes de sessão
 * 
 * Endpoints:
 * - GET /patients/{patientId}/packages → Listar pacotes do paciente
 * - GET /patient-packages/{packageId}/ledger → Histórico de créditos
 */
@Slf4j
@RestController
@RequestMapping("/patients/{patientId}/packages")
public class PatientPackageController {

    private final PackageConsumptionDomainService
        consumptionService;

    private final SessionCreditService creditService;

    private final SecurityActorFactory
        securityActorFactory;

    public PatientPackageController(
            PackageConsumptionDomainService
                    consumptionService,

            SessionCreditService creditService,

            SecurityActorFactory securityActorFactory
    ) {
        this.consumptionService = consumptionService;

        this.creditService = creditService;

        this.securityActorFactory =
                securityActorFactory;
    }

    /**
     * Lista pacotes do paciente
     * 
     * GET /patients/{patientId}/packages
     * 
     * Retorna:
     * - Pacote Essencial (8 sessões, 5 disponíveis)
     * - Status: ACTIVE
     * - Expira: 10/12/2026
     * 
     * @param patientId ID do paciente (Long)
     * @param authentication contexto de segurança
     * @param request requisição HTTP
     * @return lista de pacotes
     */
    @GetMapping
    public ResponseEntity<
            List<PatientPackageResponseDTO>
    > listPackages(
            @PathVariable Long patientId,

            Authentication authentication,

            HttpServletRequest request
    ) {

        SecurityActor actor =
                securityActorFactory
                        .from(authentication, request);

        log.info(
                "Listando pacotes: patientId={}, actor={}",
                patientId,
                actor.userId()
        );

        // TODO: Implementar lógica de listagem
        // 1. Autorizar acesso
        // 2. Buscar PatientPackages do paciente
        // 3. Calcular availableSessions via SessionCreditService.sumCreditsByPatient
        // 4. Mapear para DTOs
        // 5. Retornar lista

        return ResponseEntity.ok(List.of());
    }

    /**
     * Obtém histórico de créditos de um pacote
     * 
     * GET /patient-packages/{packageId}/ledger
     * 
     * Mostra:
     * - Ativação: +8 créditos
     * - Consumo 1: -1 crédito
     * - Consumo 2: -1 crédito
     * ...
     * 
     * @param packageId ID do pacote
     * @param authentication contexto de segurança
     * @param request requisição HTTP
     * @return histórico de movimentos
     */
    @GetMapping("/{packageId}/ledger")
    public ResponseEntity<
            List<SessionCreditEntry>
    > getLedger(
            @PathVariable UUID packageId,

            Authentication authentication,

            HttpServletRequest request
    ) {

        SecurityActor actor =
                securityActorFactory
                        .from(authentication, request);

        log.info(
                "Consultando ledger: packageId={}, actor={}",
                packageId,
                actor.userId()
        );

        // TODO: Implementar lógica de ledger
        // 1. Autorizar acesso
        // 2. Buscar SessionCreditEntry do pacote
        // 3. Retornar histórico completo

        return ResponseEntity.ok(List.of());
    }
}
