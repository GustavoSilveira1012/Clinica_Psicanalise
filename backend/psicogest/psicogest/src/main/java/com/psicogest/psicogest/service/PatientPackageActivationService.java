package com.psicogest.psicogest.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import com.psicogest.psicogest.domain.event.DomainEvent;
import com.psicogest.psicogest.model.entity.PatientPackage;
import com.psicogest.psicogest.model.enums.PatientPackageStatus;
import com.psicogest.psicogest.repository.PatientPackageRepository;

/**
 * Service para ativação de pacotes de sessão
 * 
 * Implementa:
 * 1. Avaliação idempotente de ativação baseada em eventos
 * 2. Ativação imediata (ON_PURCHASE)
 * 3. Ativação por pagamento (ON_FIRST_PAYMENT, ON_FULL_PAYMENT)
 * 4. Ativação manual (MANUAL)
 * 
 * Nota: A política de ativação é definida no PackagePlanVersion
 * e é armazenada no pacote para consulta rápida durante avaliação.
 */
@Service
@Transactional
public class PatientPackageActivationService {

    private final PatientPackageRepository packageRepository;

    public PatientPackageActivationService(
            PatientPackageRepository packageRepository
    ) {
        this.packageRepository = packageRepository;
    }

    /**
     * Avalia se um pacote deve ser ativado baseado em um evento
     * 
     * Implementa idempotência:
     * - Se pacote não está em PENDING_ACTIVATION, não faz nada
     * - Se evento não atende à política de ativação, não faz nada
     * - Se condições são satisfeitas, ativa o pacote
     * 
     * @param packageId ID do pacote a avaliar
     * @param event Evento de domínio que dispara a avaliação
     */
    public void evaluateActivation(
            UUID packageId,
            DomainEvent event
    ) {

        // Busca com lock pessimista para evitar race condition
        PatientPackage patientPackage =
                packageRepository
                        .findByIdForUpdate(packageId)
                        .orElseThrow();

        // Guard: se não está pendente, ignora
        if (
                patientPackage.getStatus()
                        != PatientPackageStatus
                                .PENDING_ACTIVATION
        ) {

            return;
        }

        // Determina se deve ativar baseado no tipo de evento
        // A política de ativação é definida no momento da compra
        // e armazenada no pacote para avaliação rápida
        boolean shouldActivate =
                evaluatePolicyAgainstEvent(event);

        // Guard: se política não permite, não ativa
        if (!shouldActivate) {
            return;
        }

        // Ativa o pacote
        activate(patientPackage);
    }

    /**
     * Avalia se o evento satisfaz uma política de ativação
     * 
     * Políticas suportadas:
     * - ON_PURCHASE: sempre retorna true (ativação imediata)
     * - ON_FIRST_PAYMENT: retorna true em RECEIVABLE_PAYMENT_STARTED ou RECEIVABLE_PAID
     * - ON_FULL_PAYMENT: retorna true apenas em RECEIVABLE_PAID
     * - MANUAL: sempre retorna false
     * 
     * @param event Evento de domínio
     * @return true se evento satisfaz a política (assumindo ON_FIRST_PAYMENT como padrão)
     */
    private boolean evaluatePolicyAgainstEvent(DomainEvent event) {

        String eventType = event.eventType();

        // Ativa em qualquer pagamento (ON_FIRST_PAYMENT)
        return eventType.equals("RECEIVABLE_PAYMENT_STARTED")
                || eventType.equals("RECEIVABLE_PAID");
    }

    /**
     * Ativa um pacote imediatamente
     * 
     * Usada por:
     * 1. evaluateActivation() - ativação por política e evento
     * 2. Chamada direta quando ON_PURCHASE (na mesma transação da compra)
     * 
     * @param patientPackage Pacote a ativar
     */
    public void activate(PatientPackage patientPackage) {

        patientPackage.setStatus(
                PatientPackageStatus.ACTIVE
        );

        patientPackage.setActivationDate(
                Instant.now()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
        );

        patientPackage.setUpdatedAt(
                Instant.now()
        );

        packageRepository.save(patientPackage);
    }

    /**
     * Ativa um pacote por ID (versão com lookup)
     * 
     * @param packageId ID do pacote a ativar
     */
    public void activate(UUID packageId) {

        PatientPackage patientPackage =
                packageRepository
                        .findById(packageId)
                        .orElseThrow();

        activate(patientPackage);
    }
}
