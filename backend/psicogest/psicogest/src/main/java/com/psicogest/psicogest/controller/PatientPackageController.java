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
import com.psicogest.psicogest.exception.ResourceNotFoundException;
import com.psicogest.psicogest.model.entity.PatientPackage;
import com.psicogest.psicogest.model.entity.SessionCreditEntry;
import com.psicogest.psicogest.repository.PatientPackageRepository;
import com.psicogest.psicogest.service.PackageAuthorizationService;
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

    private final PatientPackageRepository packageRepository;

    private final PackageAuthorizationService packageAuthorizationService;

    public PatientPackageController(
            PackageConsumptionDomainService
                    consumptionService,

            SessionCreditService creditService,

            SecurityActorFactory securityActorFactory,
            PatientPackageRepository packageRepository,
            PackageAuthorizationService packageAuthorizationService
    ) {
        this.consumptionService = consumptionService;

        this.creditService = creditService;

        this.securityActorFactory =
                securityActorFactory;
        this.packageRepository = packageRepository;
        this.packageAuthorizationService = packageAuthorizationService;
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

        packageAuthorizationService.authorizeListPackages(patientId, actor);
        return ResponseEntity.ok(packageRepository.findAllByPatient(patientId).stream()
                .map(this::toResponse)
                .toList());
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

        PatientPackage patientPackage = packageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Pacote não encontrado"));
        packageAuthorizationService.authorizeLedgerAccess(packageId, patientPackage, actor);
        return ResponseEntity.ok(creditService.getPackageHistory(packageId));
    }

    private PatientPackageResponseDTO toResponse(PatientPackage value) {
        long balance = creditService.getPackageHistory(value.getId()).stream()
                .mapToLong(entry -> entry.getDirection().name().equals("CREDIT")
                        ? entry.getSessionCount() : -entry.getSessionCount())
                .sum();
        int granted = value.getPackagePlanVersion().totalSessions();
        int available = Math.max(0, Math.toIntExact(balance));
        int consumed = Math.max(0, granted - available);
        return new PatientPackageResponseDTO(value.getId(), value.getPatient().getId(),
                value.getFinancialEntityId(), value.getPackagePlanVersion().getPackagePlan().getId(),
                value.getPackagePlanVersion().getId(), value.getPackagePlanVersion().getPackagePlan().getName(),
                value.getStatus(), value.getPurchaseAmount(), granted, consumed, available,
                value.getPurchasedAt(), value.getActivatedAt(), value.getExpiresAt());
    }
}
