package com.psicogest.psicogest.dto.saas;

import com.psicogest.psicogest.model.enums.SaasInvoiceStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SaasInvoiceResponse(
        UUID id,
        SaasInvoiceStatus status,
        BigDecimal amount,
        String currency,
        Instant dueAt,
        Instant paidAt,
        String hostedInvoiceUrl
) {
}
