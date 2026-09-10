package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.Receivable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
