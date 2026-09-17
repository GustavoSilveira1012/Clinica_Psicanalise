package com.psicogest.psicogest.dto.saas;

import com.psicogest.psicogest.model.enums.SaasSubscriptionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SaasBillingResponse(
        UUID organizationId,
        String planCode,
        String planName,
        SaasSubscriptionStatus status,
        BigDecimal monthlyPrice,
        Instant trialEndsAt,
        LocalDate currentPeriodEnd,
        List<SaasInvoiceResponse> invoices
) {
}
