package com.psicogest.psicogest.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

import com.psicogest.psicogest.domain.event.DomainEvent;
import com.psicogest.psicogest.model.entity.PatientPackage;
import com.psicogest.psicogest.repository.PatientPackageRepository;

/**
 * Processador de eventos para ativação de pacotes
 * 
 * Escuta eventos de cobrança (RECEIVABLE_PAYMENT_STARTED, RECEIVABLE_PAID)
 * e coordena a ativação de pacotes baseado na política de ativação.
 * 
 * Responsabilidades:
 * 1. Buscar pacotes associados ao receivable
 * 2. Delegar avaliação de ativação para o service
 * 3. Implementar idempotência (mesmo evento processado múltiplas vezes)
 */
@Component
public class PackageActivationProcessor {

    private final PatientPackageRepository patientPackageRepository;

    private final PatientPackageActivationService activationService;

    public PackageActivationProcessor(
            PatientPackageRepository patientPackageRepository,
            PatientPackageActivationService activationService
    ) {
        this.patientPackageRepository = patientPackageRepository;
        this.activationService = activationService;
    }

    /**
     * Processa evento de domínio para ativar pacotes relacionados
     * 
     * @param event Evento de domínio (ex: RECEIVABLE_PAYMENT_STARTED, RECEIVABLE_PAID)
     */
    public void handle(DomainEvent event) {

        UUID receivableId = UUID.fromString(
                event.aggregateId()
        );

        // Busca todos os pacotes vinculados a essa cobrança
        List<PatientPackage> packages =
                patientPackageRepository
                        .findByReceivableId(receivableId);

        // Avalia cada pacote para possível ativação
        for (PatientPackage patientPackage : packages) {

            activationService.evaluateActivation(
                    patientPackage.getId(),
                    event
            );
        }
    }
}
