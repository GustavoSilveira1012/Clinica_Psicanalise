package com.psicogest.psicogest.infrastructure.saas.billing;

import com.psicogest.psicogest.model.enums.SaasSubscriptionStatus;

import java.time.Instant;

/** Estado normalizado retornado por um provedor de billing SaaS. */
public record SaasSubscriptionSnapshot(
        String providerSubscriptionId,
        SaasSubscriptionStatus status,
        Instant currentPeriodStart,
        Instant currentPeriodEnd,
        boolean cancelAtPeriodEnd
) {
}
