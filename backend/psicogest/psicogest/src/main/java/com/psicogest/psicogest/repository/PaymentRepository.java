package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository para pagamentos
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
}
