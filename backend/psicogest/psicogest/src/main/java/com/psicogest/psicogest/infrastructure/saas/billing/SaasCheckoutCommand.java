package com.psicogest.psicogest.infrastructure.saas.billing;

import java.util.UUID;

/** Dados mínimos para abrir um checkout, sem expor entidades clínicas ao gateway. */
public record SaasCheckoutCommand(
        UUID organizationId,
        UUID subscriptionId,
        String planCode,
        String billingInterval,
        String customerReference,
        String idempotencyKey
) {
}
