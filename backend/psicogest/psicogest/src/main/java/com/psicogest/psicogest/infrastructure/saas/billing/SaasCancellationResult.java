package com.psicogest.psicogest.infrastructure.saas.billing;

import java.time.Instant;

public record SaasCancellationResult(
        String providerSubscriptionId,
        boolean cancelled,
        Instant effectiveAt
) {
}
