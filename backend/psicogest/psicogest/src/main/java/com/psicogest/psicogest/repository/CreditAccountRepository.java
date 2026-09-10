package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.CreditAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository para contas de crédito
 */
@Repository
public interface CreditAccountRepository
        extends JpaRepository<CreditAccount, UUID> {

    /**
     * Busca conta de crédito com lock pessimista
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT ca
            FROM CreditAccount ca
            WHERE ca.id = :accountId
            """)
    Optional<CreditAccount> findByIdForUpdate(
            @Param("accountId")
            UUID accountId
    );

    /**
     * Busca ou cria conta de crédito para um contexto
     * (patient, clinic, currency)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT ca
            FROM CreditAccount ca
            WHERE ca.patient.id = :patientId
              AND ca.clinic.id = :clinicId
              AND ca.currency = :currency
            """)
    Optional<CreditAccount> findForUpdate(
            @Param("patientId")
            Long patientId,
            @Param("clinicId")
            Long clinicId,
            @Param("currency")
            String currency
    );

    /**
     * Busca conta sem lock (leitura)
     */
    @Query("""
            SELECT ca
            FROM CreditAccount ca
            WHERE ca.patient.id = :patientId
              AND ca.clinic.id = :clinicId
              AND ca.currency = :currency
            """)
    Optional<CreditAccount> findByContext(
            @Param("patientId")
            Long patientId,
            @Param("clinicId")
            Long clinicId,
            @Param("currency")
            String currency
    );
}
