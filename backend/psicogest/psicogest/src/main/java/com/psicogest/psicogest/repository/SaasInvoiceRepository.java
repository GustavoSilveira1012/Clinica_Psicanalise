package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.saas.SaasInvoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SaasInvoiceRepository extends JpaRepository<SaasInvoice, UUID> {
    List<SaasInvoice> findTop12ByOrganizationIdOrderByDueAtDesc(UUID organizationId);
}
