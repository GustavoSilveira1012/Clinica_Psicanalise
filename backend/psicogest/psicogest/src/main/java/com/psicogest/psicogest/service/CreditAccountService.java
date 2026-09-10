package com.psicogest.psicogest.service;

import com.psicogest.psicogest.model.entity.Clinic;
import com.psicogest.psicogest.model.entity.CreditAccount;
import com.psicogest.psicogest.model.entity.Patient;
import com.psicogest.psicogest.repository.CreditAccountRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Service para gerenciamento de contas de crédito
 */
@Slf4j
@Service
@Transactional
public class CreditAccountService {

    private final CreditAccountRepository creditAccountRepository;
    private final Clock clock;

    public CreditAccountService(
            CreditAccountRepository creditAccountRepository,
            Clock clock
    ) {
        this.creditAccountRepository = creditAccountRepository;
        this.clock = clock;
    }

    /**
     * Busca ou cria conta de crédito para um contexto
     * (patient, clinic, currency)
     * 
     * @param patient paciente
     * @param clinic clínica (pode ser null)
     * @param currency moeda
     * @return conta de crédito
     */
    @Transactional
    public CreditAccount getOrCreate(
            Patient patient,
            Clinic clinic,
            String currency
    ) {

        Long clinicId = clinic != null
                ? clinic.getId()
                : null;

        Long patientId = patient.getId();

        // Tentar encontrar com lock
        var existing =
                creditAccountRepository
                        .findForUpdate(
                                patientId,
                                clinicId,
                                currency
                        );

        if (existing.isPresent()) {

            return existing.get();
        }

        // Criar nova
        CreditAccount account =
                CreditAccount.builder()

                        .id(UUID.randomUUID())

                        .patient(patient)

                        .clinic(clinic)

                        .currency(currency)

                        .createdAt(
                                clock.instant()
                        )

                        .build();

        CreditAccount saved =
                creditAccountRepository.saveAndFlush(
                        account
                );

        log.info(
                "CreditAccount criada: id={}, patient={}, clinic={}, currency={}",
                saved.getId(),
                patientId,
                clinicId,
                currency
        );

        return saved;
    }
}
