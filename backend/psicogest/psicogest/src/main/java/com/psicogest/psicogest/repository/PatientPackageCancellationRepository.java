package com.psicogest.psicogest.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.psicogest.psicogest.model.entity.PatientPackageCancellation;
import com.psicogest.psicogest.model.enums.PackageCancellationStatus;

import jakarta.persistence.LockModeType;

/**
 * Repository para PatientPackageCancellation
 * 
 * Gerencia histórico de cancelamentos de pacotes
 */
@Repository
public interface PatientPackageCancellationRepository
        extends JpaRepository<PatientPackageCancellation, UUID> {

    /**
     * Encontra cancelamento para atualização com lock pessimista
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT c
        FROM PatientPackageCancellation c
        WHERE c.id = :cancellationId
    """)
    Optional<PatientPackageCancellation> findByIdForUpdate(
        @Param("cancellationId")
        UUID cancellationId
    );

    /**
     * Encontra cancelamentos de um pacote
     */
    @Query("""
        SELECT c
        FROM PatientPackageCancellation c
        WHERE c.patientPackage.id = :packageId
        ORDER BY c.createdAt DESC
    """)
    List<PatientPackageCancellation> findByPatientPackageId(
        @Param("packageId")
        UUID packageId
    );

    /**
     * Encontra cancelamentos com status específico
     */
    @Query("""
        SELECT c
        FROM PatientPackageCancellation c
        WHERE c.status = :status
        ORDER BY c.createdAt DESC
    """)
    List<PatientPackageCancellation> findByStatus(
        @Param("status")
        PackageCancellationStatus status
    );

    /**
     * Encontra cancelamentos pendentes (CALCULATED ou SETTLING)
     */
    @Query("""
        SELECT c
        FROM PatientPackageCancellation c
        WHERE c.patientPackage.id = :packageId
          AND (c.status = 'CALCULATED' OR c.status = 'SETTLING')
    """)
    List<PatientPackageCancellation> findPendingByPatientPackageId(
        @Param("packageId")
        UUID packageId
    );
}
