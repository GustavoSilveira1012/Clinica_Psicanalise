package com.psicogest.psicogest.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.psicogest.psicogest.model.entity.PatientPackage;
import com.psicogest.psicogest.model.enums.PatientPackageStatus;

import jakarta.persistence.LockModeType;

@Repository
public interface PatientPackageRepository
        extends JpaRepository<PatientPackage, UUID> {

    /**
     * Lista pacotes de um paciente
     */
    @Query("""
        SELECT p
        FROM PatientPackage p
        WHERE p.patient.id = :patientId
        ORDER BY p.createdAt DESC
    """)
    List<PatientPackage> findAllByPatient(
        @Param("patientId")
        Long patientId
    );

    /**
     * Encontra pacote para atualização com lock pessimista
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p
        FROM PatientPackage p
        WHERE p.id = :packageId
    """)
    Optional<PatientPackage> findByIdForUpdate(
        @Param("packageId")
        UUID packageId
    );

    /**
     * Lista pacotes ativos de um paciente
     */
    @Query("""
        SELECT p
        FROM PatientPackage p
        WHERE p.patient.id = :patientId
          AND p.status = 'ACTIVE'
          AND p.availableSessions > 0
        ORDER BY p.expirationDate ASC
    """)
    List<PatientPackage> findActiveByPatient(
        @Param("patientId")
        Long patientId
    );

    /**
     * Encontra pacotes ativos elegíveis para consumo
     * 
     * Critérios:
     * - Status ACTIVE
     * - Mesmo paciente
     * - Mesma entidade financeira
     * - Não expirado (expirationDate > now)
     * - Saldo disponível (availableSessions > 0)
     * 
     * Ordenado por FEFO: expiração primeiro, depois compra
     */
    @Query("""
        SELECT p
        FROM PatientPackage p
        WHERE p.patient.id = :patientId
          AND p.financialEntityId = :financialEntityId
          AND p.status = 'ACTIVE'
          AND p.availableSessions > 0
          AND (p.expirationDate IS NULL OR 
               CAST(p.expirationDate AS java.time.Instant) > :now)
        ORDER BY p.expirationDate ASC,
                 p.createdAt ASC,
                 p.id ASC
    """)
    List<PatientPackage> findActivePackagesForPatient(
        @Param("patientId")
        Long patientId,

        @Param("financialEntityId")
        Long financialEntityId,

        @Param("now")
        Instant now
    );

    /**
     * Conta pacotes ativos de um paciente
     */
    @Query("""
        SELECT COUNT(p)
        FROM PatientPackage p
        WHERE p.patient.id = :patientId
          AND p.status = :status
    """)
    long countByPatientAndStatus(
        @Param("patientId")
        Long patientId,

        @Param("status")
        PatientPackageStatus status
    );

    /**
     * Encontra pacotes vinculados a uma cobrança (receivable)
     * 
     * Usado pelo PackageActivationProcessor para localizar
     * pacotes a serem ativados quando há evento de pagamento
     */
    @Query("""
        SELECT p
        FROM PatientPackage p
        WHERE p.receivableId = :receivableId
    """)
    List<PatientPackage> findByReceivableId(
        @Param("receivableId")
        UUID receivableId
    );
}
