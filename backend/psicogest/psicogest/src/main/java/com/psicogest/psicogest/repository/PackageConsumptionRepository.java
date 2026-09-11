package com.psicogest.psicogest.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.psicogest.psicogest.model.entity.PackageConsumption;
import com.psicogest.psicogest.model.enums.PackageConsumptionStatus;

@Repository
public interface PackageConsumptionRepository
        extends JpaRepository<PackageConsumption, UUID> {

    /**
     * Lista consumos de um pacote
     */
    @Query("""
        SELECT pc
        FROM PackageConsumption pc
        WHERE pc.patientPackage.id = :packageId
        ORDER BY pc.consumedAt DESC
    """)
    List<PackageConsumption> findAllByPackage(
        @Param("packageId")
        UUID packageId
    );

    /**
     * Lista consumos ativos de um item em um pacote
     */
    @Query("""
        SELECT pc
        FROM PackageConsumption pc
        WHERE pc.patientPackage.id = :packageId
          AND pc.packageItemId = :itemId
          AND pc.status = 'ACTIVE'
        ORDER BY pc.consumedAt DESC
    """)
    List<PackageConsumption> findActiveByPackageAndItem(
        @Param("packageId")
        UUID packageId,

        @Param("itemId")
        UUID itemId
    );

    /**
     * Calcula saldo de um item em um pacote
     * 
     * Saldo = COUNT(ACTIVE) - COUNT(REVERSED)
     * 
     * Exemplos:
     * - 8 ACTIVE, 3 REVERSED = 5 de saldo
     * - 5 ACTIVE, 0 REVERSED = 5 de saldo
     */
    @Query("""
        SELECT COALESCE(
            SUM(
                CASE
                    WHEN pc.status = 'ACTIVE' THEN 1
                    WHEN pc.status = 'REVERSED' THEN -1
                    ELSE 0
                END
            ),
            0
        )
        FROM PackageConsumption pc
        WHERE pc.patientPackage.id = :packageId
          AND pc.packageItemId = :itemId
    """)
    Long calculateBalance(
        @Param("packageId")
        UUID packageId,

        @Param("itemId")
        UUID itemId
    );

    /**
     * Conta consumos ativos de um item
     */
    @Query("""
        SELECT COUNT(pc)
        FROM PackageConsumption pc
        WHERE pc.patientPackage.id = :packageId
          AND pc.packageItemId = :itemId
          AND pc.status = 'ACTIVE'
    """)
    long countActiveByPackageAndItem(
        @Param("packageId")
        UUID packageId,

        @Param("itemId")
        UUID itemId
    );

    /**
     * Encontra consumo por agendamento
     */
    @Query("""
        SELECT pc
        FROM PackageConsumption pc
        WHERE pc.appointment.id = :appointmentId
    """)
    List<PackageConsumption> findByAppointment(
        @Param("appointmentId")
        UUID appointmentId
    );
}
