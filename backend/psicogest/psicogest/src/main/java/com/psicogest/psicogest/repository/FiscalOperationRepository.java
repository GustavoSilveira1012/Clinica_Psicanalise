package com.psicogest.psicogest.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.psicogest.psicogest.model.entity.FiscalOperation;

@Repository
public interface FiscalOperationRepository
        extends JpaRepository<FiscalOperation, UUID> {

    /**
     * Encontra operação pela chave de idempotência
     */
    Optional<FiscalOperation> findByIdempotencyKey(String idempotencyKey);
}
