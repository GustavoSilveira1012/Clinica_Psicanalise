package com.psicogest.psicogest.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.psicogest.psicogest.model.entity.FiscalConfiguration;

@Repository
public interface FiscalConfigurationRepository
        extends JpaRepository<FiscalConfiguration, UUID> {

    /**
     * Encontra configuração fiscal efetiva para uma competência
     * 
     * Retorna a configuração que está ativa na data especificada
     */
    @Query("""
        SELECT fc
        FROM FiscalConfiguration fc
        WHERE fc.issuer.id = :issuerId
          AND fc.active = true
          AND fc.validityStart <= :competenceDate
          AND (fc.validityEnd IS NULL OR fc.validityEnd >= :competenceDate)
        ORDER BY fc.validityStart DESC
        LIMIT 1
    """)
    Optional<FiscalConfiguration> findEffective(
        UUID issuerId,
        LocalDate competenceDate
    );
}
