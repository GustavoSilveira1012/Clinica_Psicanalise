package com.psicogest.psicogest.repository;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.psicogest.psicogest.model.entity.ProviderSettlement;

/**
 * Repository para repasses de gateway
 */
@Repository
public interface ProviderSettlementRepository
        extends JpaRepository<ProviderSettlement, UUID> {

    /**
     * Busca com lock pessimista (PESSIMISTIC_WRITE)
     * Para operações críticas de validação e atualização
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProviderSettlement p WHERE p.id = :id")
    Optional<ProviderSettlement> findByIdForUpdate(UUID id);
}
