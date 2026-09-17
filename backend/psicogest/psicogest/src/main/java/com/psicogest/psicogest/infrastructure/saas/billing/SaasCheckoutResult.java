package com.psicogest.psicogest.infrastructure.saas.billing;

import java.time.Instant;

/** Resultado neutro do checkout; dados sensíveis do cartão ficam no gateway. */
public record SaasCheckoutResult(
        String providerSubscriptionId,
        String checkoutUrl,
        Instant expiresAt
) {
}
