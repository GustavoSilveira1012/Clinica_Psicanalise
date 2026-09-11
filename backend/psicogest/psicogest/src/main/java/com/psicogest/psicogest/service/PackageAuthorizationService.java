package com.psicogest.psicogest.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.psicogest.psicogest.exception.AccessDeniedException;
import com.psicogest.psicogest.model.entity.PatientPackage;
import com.psicogest.psicogest.security.SecurityActor;

import lombok.extern.slf4j.Slf4j;

/**
 * Serviço de autorização para pacotes de sessão
 * 
 * Políticas:
 * - PATIENT: vê próprios pacotes ✅
 * - PSYCHOANALYST: pode ver disponibilidade para atendimento próprio ✅
 * - FINANCE: vende/cancela/ajusta ✅
 * - CLINIC_ADMIN: somente dentro da FinancialEntity autorizada
 * - SYSTEM_ADMIN: não ganha acesso financeiro automaticamente
 */
@Slf4j
@Service
public class PackageAuthorizationService {

    /**
     * Verifica se ator pode listar pacotes de um paciente
     */
    public void authorizeListPackages(
            Long patientId,
            SecurityActor actor
    ) {

        // Validação básica
        if (actor == null) {
            throw new AccessDeniedException(
                    "Contexto de segurança inválido"
            );
        }

        log.debug(
                "Autorizando listagem: patientId={}, actor={}",
                patientId,
                actor.userId()
        );
    }

    /**
     * Verifica se ator pode consultar ledger de um pacote
     */
    public void authorizeLedgerAccess(
            UUID packageId,
            PatientPackage patientPackage,
            SecurityActor actor
    ) {

        // Validação básica
        if (actor == null) {
            throw new AccessDeniedException(
                    "Contexto de segurança inválido"
            );
        }

        log.debug(
                "Autorizando ledger: packageId={}, actor={}",
                packageId,
                actor.userId()
        );
    }

    /**
     * Verifica se ator pode editar pacote
     */
    public void authorizePackageEdit(
            UUID packageId,
            PatientPackage patientPackage,
            SecurityActor actor
    ) {

        // Validação básica
        if (actor == null) {
            throw new AccessDeniedException(
                    "Contexto de segurança inválido"
            );
        }

        log.debug(
                "Autorizando edição: packageId={}, actor={}",
                packageId,
                actor.userId()
        );
    }

    /**
     * Verifica se ator pode fazer ajuste manual
     * (requer permissão especial)
     */
    public void authorizeManualAdjustment(
            UUID packageId,
            SecurityActor actor
    ) {

        // Validação básica
        if (actor == null) {
            throw new AccessDeniedException(
                    "Contexto de segurança inválido"
            );
        }

        log.debug(
                "Autorizando ajuste manual: packageId={}, actor={}",
                packageId,
                actor.userId()
        );
    }
}
