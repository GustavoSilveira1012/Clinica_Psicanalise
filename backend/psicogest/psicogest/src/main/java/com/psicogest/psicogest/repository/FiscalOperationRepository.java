package com.psicogest.psicogest.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import com.psicogest.psicogest.model.entity.FiscalOperation;

@Repository
public interface FiscalOperationRepository
        extends JpaRepository<FiscalOperation, UUID> {

    /**
     * Encontra operação pela chave de idempotência
     */
    Optional<FiscalOperation> findByIdempotencyKey(String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select operation from FiscalOperation operation join fetch operation.invoice where operation.id = :id")
    Optional<FiscalOperation> findByIdForUpdate(@Param("id") UUID id);
}
