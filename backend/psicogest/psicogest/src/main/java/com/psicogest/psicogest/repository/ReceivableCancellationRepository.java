package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.ReceivableCancellation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository para cancelamentos de cobrança
 */
@Repository
public interface ReceivableCancellationRepository
        extends JpaRepository<ReceivableCancellation, UUID> {

    /**
     * Busca cancelamento com lock pessimista
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT rc
            FROM ReceivableCancellation rc
            WHERE rc.id = :cancellationId
            """)
    Optional<ReceivableCancellation> findByIdForUpdate(
            @Param("cancellationId")
            UUID cancellationId
    );

    /**
     * Busca cancelamento por cobrança
     */
    Optional<ReceivableCancellation> findByReceivableId(
            UUID receivableId
    );
}
