package com.psicogest.psicogest.service.saas;

import com.psicogest.psicogest.dto.saas.SaasBillingResponse;
import com.psicogest.psicogest.dto.saas.SaasInvoiceResponse;
import com.psicogest.psicogest.exception.ResourceNotFoundException;
import com.psicogest.psicogest.model.entity.saas.SaasSubscription;
import com.psicogest.psicogest.repository.SaasInvoiceRepository;
import com.psicogest.psicogest.repository.SaasSubscriptionRepository;
import com.psicogest.psicogest.security.tenant.TenantContextResolver;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** SaaS billing is intentionally separate from clinic receivables/payments. */
@Service
public class SaasBillingService {

    private final TenantContextResolver tenantContextResolver;
    private final SaasSubscriptionRepository subscriptionRepository;
    private final SaasInvoiceRepository invoiceRepository;

    public SaasBillingService(
            TenantContextResolver tenantContextResolver,
            SaasSubscriptionRepository subscriptionRepository,
            SaasInvoiceRepository invoiceRepository
    ) {
        this.tenantContextResolver = tenantContextResolver;
        this.subscriptionRepository = subscriptionRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional(readOnly = true)
    public SaasBillingResponse summary(UUID organizationId, Authentication authentication) {
        tenantContextResolver.resolve(authentication, organizationId);
        SaasSubscription subscription = subscriptionRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura SaaS não encontrada"));
        return new SaasBillingResponse(
                organizationId,
                subscription.getPlanVersion().getPlan().getCode(),
                subscription.getPlanVersion().getPlan().getName(),
                subscription.getStatus(),
                subscription.getPlanVersion().getMonthlyPrice(),
                subscription.getTrialEndsAt(),
                subscription.getCurrentPeriodEnd(),
                invoiceRepository.findTop12ByOrganizationIdOrderByDueAtDesc(organizationId)
                        .stream()
                        .map(invoice -> new SaasInvoiceResponse(
                                invoice.getId(), invoice.getStatus(), invoice.getAmount(),
                                invoice.getCurrency(), invoice.getDueAt(), invoice.getPaidAt(),
                                invoice.getHostedInvoiceUrl()))
                        .toList());
    }
}
