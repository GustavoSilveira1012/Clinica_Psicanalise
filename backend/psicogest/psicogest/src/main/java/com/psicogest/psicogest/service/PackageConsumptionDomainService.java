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
import com.psicogest.psicogest.model.entity.SessionCreditEntry;
import com.psicogest.psicogest.model.enums.PackageConsumptionStatus;
import com.psicogest.psicogest.model.enums.SessionCreditDirection;
import com.psicogest.psicogest.model.enums.SessionCreditEntryType;
import com.psicogest.psicogest.repository.PackageConsumptionRepository;
import com.psicogest.psicogest.repository.PatientPackageRepository;
import com.psicogest.psicogest.repository.SessionCreditEntryRepository;

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

    private final SessionCreditEntryRepository creditEntryRepository;

    private final PatientPackageRepository
        packageRepository;

    private final PackageConsumptionPolicy policy;

    private final Clock clock;

    public PackageConsumptionDomainService(
            PackageConsumptionRepository
                    consumptionRepository,

            SessionCreditEntryRepository creditEntryRepository,

            PatientPackageRepository packageRepository,

            PackageConsumptionPolicy policy,

            Clock clock
    ) {
        this.consumptionRepository =
                consumptionRepository;

        this.creditEntryRepository = creditEntryRepository;

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

        return consumeSession(appointment, patientId, (UUID) null, packageItemId);
    }

    public boolean consumeSession(
            Appointment appointment,
            Long patientId,
            UUID financialEntityId,
            UUID packageItemId
    ) {

        if (appointment == null || appointment.getPatient() == null
                || patientId == null || !patientId.equals(appointment.getPatient().getId())
                || packageItemId == null) {
            return false;
        }

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

        // Seleciona o primeiro pacote FEFO que contém o item e ainda tem saldo.
        PatientPackage selectedPackage = eligiblePackages.stream()
                .filter(candidate -> candidate.getPackagePlanVersion().getItems().stream()
                        .anyMatch(item -> item.getId().equals(packageItemId)))
                .filter(candidate -> {
                    var item = candidate.getPackagePlanVersion().getItems().stream()
                            .filter(value -> value.getId().equals(packageItemId))
                            .findFirst().orElseThrow();
                    return consumptionRepository.countActiveByPackageAndItem(candidate.getId(), packageItemId)
                            < item.getQuantity();
                })
                .findFirst()
                .orElse(null);

        if (selectedPackage == null) {
            log.warn("Nenhum pacote com saldo elegível: patientId={}, itemId={}", patientId, packageItemId);
            return false;
        }

        log.info(
                "Consumindo sessão FEFO: " +
                "packageId={}, itemId={}, " +
                "expiresAt={}",
                selectedPackage.getId(),
                packageItemId,
                selectedPackage.getExpirationDate()
        );

        // O débito imutável é gravado antes do vínculo que o referencia.
        SessionCreditEntry debit = creditEntryRepository.saveAndFlush(SessionCreditEntry.builder()
                .id(UUID.randomUUID())
                .patient(appointment.getPatient())
                .patientPackage(selectedPackage)
                .packageItemId(packageItemId)
                .entryType(SessionCreditEntryType.APPOINTMENT_CONSUMPTION)
                .direction(SessionCreditDirection.DEBIT)
                .sessionCount(1L)
                .appointmentId(appointment.getId())
                .createdAt(now)
                .build());

        // Passo 4: Registra consumo associado ao débito do ledger.
        PackageConsumption consumption =
                PackageConsumption
                        .builder()

                        .id(UUID.randomUUID())

                        .appointment(appointment)

                        .patientPackage(
                                selectedPackage
                        )

                        .packageItemId(packageItemId)

                        .debitEntry(debit)

                        .status(
                                PackageConsumptionStatus
                                        .ACTIVE
                        )

                        .consumedAt(now)

                        .createdAt(now)

                        .build();

        consumptionRepository.save(consumption);

        log.info(
                "Sessão consumida com sucesso: " +
                "appointmentId={}, packageId={}, debitEntryId={}",
                appointment.getId(),
                selectedPackage.getId(),
                debit.getId()
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
            Long appointmentId,
            String reason
    ) {

        Instant now = clock.instant();

        List<PackageConsumption> consumptions =
                consumptionRepository
                        .findActiveByAppointmentForUpdate(appointmentId);

        for (PackageConsumption consumption :
                consumptions) {

            if (consumption.getStatus() !=
                    PackageConsumptionStatus
                            .ACTIVE) {

                continue;
            }

            SessionCreditEntry reversal = creditEntryRepository.saveAndFlush(SessionCreditEntry.builder()
                    .id(UUID.randomUUID())
                    .patient(consumption.getPatientPackage().getPatient())
                    .patientPackage(consumption.getPatientPackage())
                    .packageItemId(consumption.getPackageItemId())
                    .entryType(SessionCreditEntryType.CONSUMPTION_REVERSAL)
                    .direction(SessionCreditDirection.CREDIT)
                    .sessionCount(1L)
                    .appointmentId(appointmentId)
                    .reversesEntryId(consumption.getDebitEntry().getId())
                    .reason(reason)
                    .createdAt(now)
                    .build());

            // Marca o consumo operacional como revertido; o saldo vem do ledger.
            consumption.reverse(reason, now, reversal);

            consumptionRepository.save(consumption);

            PatientPackage packageEntity = consumption.getPatientPackage();

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
                    Long appointmentId
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
