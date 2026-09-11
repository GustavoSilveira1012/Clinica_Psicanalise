package com.psicogest.psicogest.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.psicogest.psicogest.model.entity.Appointment;
import com.psicogest.psicogest.model.entity.PackageConsumption;
import com.psicogest.psicogest.model.entity.PatientPackage;
import com.psicogest.psicogest.model.enums.PackageConsumptionStatus;
import com.psicogest.psicogest.repository.PackageConsumptionRepository;
import com.psicogest.psicogest.repository.PatientPackageRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Serviço de domínio para consumo de pacotes
 * 
 * Orquestra:
 * 1. Verificação de política (shouldConsume)
 * 2. Busca de pacotes elegíveis (findActivePackagesForPatient)
 * 3. Seleção FEFO (First Expire, First Out)
 * 4. Registro de consumo
 */
@Slf4j
@Service
@Transactional
public class PackageConsumptionDomainService {

    private final PackageConsumptionRepository
        consumptionRepository;

    private final PatientPackageRepository
        packageRepository;

    private final PackageConsumptionPolicy policy;

    private final Clock clock;

    public PackageConsumptionDomainService(
            PackageConsumptionRepository
                    consumptionRepository,

            PatientPackageRepository packageRepository,

            PackageConsumptionPolicy policy,

            Clock clock
    ) {
        this.consumptionRepository =
                consumptionRepository;

        this.packageRepository = packageRepository;

        this.policy = policy;

        this.clock = clock;
    }

    /**
     * Consome uma sessão de um pacote
     * 
     * Fluxo:
     * 1. Verifica se deve consumir via política
     * 2. Busca pacotes elegíveis ordenados FEFO
     * 3. Seleciona primeiro disponível
     * 4. Registra consumo e atualiza saldo
     * 
     * @return true se consumiu, false caso contrário
     */
    public boolean consumeSession(
            Appointment appointment,
            Long patientId,
            Long financialEntityId,
            UUID packageItemId
    ) {

        Instant now = clock.instant();

        // Passo 1: Verifica política
        if (!policy.shouldConsume(
                appointment,
                null,
                now
        )) {

            log.debug(
                    "Consumo não permitido pela política: " +
                    "appointmentId={}, status={}",
                    appointment.getId(),
                    appointment.getStatus()
            );

            return false;
        }

        // Passo 2: Busca pacotes elegíveis (FEFO)
        List<PatientPackage> eligiblePackages =
                packageRepository
                        .findActivePackagesForPatient(
                                patientId,
                                financialEntityId,
                                now
                        );

        if (eligiblePackages.isEmpty()) {

            log.warn(
                    "Nenhum pacote elegível para consumo: " +
                    "patientId={}, appointmentId={}",
                    patientId,
                    appointment.getId()
            );

            return false;
        }

        // Passo 3: Seleciona primeiro (FEFO)
        PatientPackage selectedPackage =
                eligiblePackages.get(0);

        log.info(
                "Consumindo sessão FEFO: " +
                "packageId={}, itemId={}, " +
                "expiresAt={}",
                selectedPackage.getId(),
                packageItemId,
                selectedPackage.getExpirationDate()
        );

        // Passo 4: Registra consumo
        PackageConsumption consumption =
                PackageConsumption
                        .builder()

                        .id(UUID.randomUUID())

                        .appointment(appointment)

                        .patientPackage(
                                selectedPackage
                        )

                        .packageItemId(packageItemId)

                        .status(
                                PackageConsumptionStatus
                                        .ACTIVE
                        )

                        .consumedAt(now)

                        .createdAt(now)

                        .build();

        consumptionRepository.save(consumption);

        // Atualiza saldo do pacote
        selectedPackage.consumeSession();

        packageRepository.save(selectedPackage);

        log.info(
                "Sessão consumida com sucesso: " +
                "appointmentId={}, packageId={}, " +
                "novoSaldo={}",
                appointment.getId(),
                selectedPackage.getId(),
                selectedPackage.getAvailableSessions()
        );

        return true;
    }

    /**
     * Reverte o consumo de uma sessão
     * 
     * Encontra o PackageConsumption associado
     * ao agendamento e reverte ambos
     */
    public void reverseConsumption(
            UUID appointmentId,
            String reason
    ) {

        Instant now = clock.instant();

        List<PackageConsumption> consumptions =
                consumptionRepository
                        .findByAppointment(appointmentId);

        for (PackageConsumption consumption :
                consumptions) {

            if (consumption.getStatus() !=
                    PackageConsumptionStatus
                            .ACTIVE) {

                continue;
            }

            // Reverter consumo
            consumption.reverse(reason, now);

            consumptionRepository.save(consumption);

            // Restaurar saldo do pacote
            PatientPackage packageEntity =
                    consumption.getPatientPackage();

            packageEntity.reverseSession();

            packageRepository.save(packageEntity);

            log.info(
                    "Consumo revertido: " +
                    "appointmentId={}, packageId={}, " +
                    "reason={}",
                    appointmentId,
                    packageEntity.getId(),
                    reason
            );
        }
    }

    /**
     * Encontra consumo por agendamento
     */
    public Optional<PackageConsumption>
            findByAppointment(
                    UUID appointmentId
            ) {

        List<PackageConsumption> consumptions =
                consumptionRepository
                        .findByAppointment(
                                appointmentId
                        );

        return consumptions.stream()
                .filter(c -> c.getStatus() ==
                        PackageConsumptionStatus
                                .ACTIVE)
                .findFirst();
    }
}
