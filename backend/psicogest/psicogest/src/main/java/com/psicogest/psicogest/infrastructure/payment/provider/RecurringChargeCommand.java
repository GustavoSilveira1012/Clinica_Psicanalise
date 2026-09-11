package com.psicogest.psicogest.infrastructure.payment.provider;

import java.math.BigDecimal;
import java.util.UUID;

public record RecurringChargeCommand(
        UUID subscriptionId,
        UUID cycleId,
        BigDecimal amount,
        String currency,
        String providerCustomerReference,
        String providerPaymentMethodReference,
        String idempotencyKey
) {
}
