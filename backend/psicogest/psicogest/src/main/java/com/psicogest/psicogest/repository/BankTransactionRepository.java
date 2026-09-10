package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para transações bancárias
 */
@Repository
public interface BankTransactionRepository
        extends JpaRepository<BankTransaction, UUID> {

    /**
     * Verifica duplicação por fingerprint
     */
    boolean existsByBankAccountIdAndTransactionFingerprint(
            UUID bankAccountId,
            String fingerprint
    );

    /**
     * Verifica duplicação por externalId
     */
    boolean existsByBankAccountIdAndExternalTransactionId(
            UUID bankAccountId,
            String externalId
    );

    /**
     * Busca com lock pessimista (PESSIMISTIC_WRITE)
     * Impede race conditions em reconciliação
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BankTransaction b WHERE b.id = :id")
    Optional<BankTransaction> findByIdForUpdate(UUID id);
}

