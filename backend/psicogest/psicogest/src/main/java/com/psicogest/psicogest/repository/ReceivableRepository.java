package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.Receivable;

import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository para contas a receber
 */
@Repository
public interface ReceivableRepository extends JpaRepository<Receivable, UUID> {

    /**
     * Verifica se existe cobrança para uma consulta
     */
    boolean existsByAppointmentId(Long appointmentId);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("""
        SELECT r
        FROM Receivable r
        WHERE r.id = :receivableId
        """)
Optional<Receivable> findByIdForUpdate(
        @Param("receivableId")
        UUID receivableId
);
}
