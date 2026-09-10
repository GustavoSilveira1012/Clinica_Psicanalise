package com.psicogest.psicogest.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.psicogest.psicogest.model.entity.FiscalIssuer;

@Repository
public interface FiscalIssuerRepository
        extends JpaRepository<FiscalIssuer, UUID> {

    /**
     * Encontra emissor ativo por ID
     */
    @Query("""
        SELECT fi
        FROM FiscalIssuer fi
        WHERE fi.id = :id
          AND fi.active = true
    """)
    Optional<FiscalIssuer> findActiveById(UUID id);
}
