package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository para contas bancárias
 */
@Repository
public interface BankAccountRepository
        extends JpaRepository<BankAccount, UUID> {

    /**
     * Busca contas ativas de uma clínica
     */
    List<BankAccount> findByClinicIdAndActiveTrue(UUID clinicId);

    /**
     * Verifica se existe conta ativa
     */
    boolean existsByIdAndActiveTrue(UUID bankAccountId);
}
