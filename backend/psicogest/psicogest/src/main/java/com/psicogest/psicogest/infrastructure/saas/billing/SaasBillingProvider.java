package com.psicogest.psicogest.infrastructure.saas.billing;

import java.util.Optional;

/**
 * Porta de billing do SaaS. Ela é deliberadamente separada dos pagamentos
 * realizados pelos pacientes para a clínica.
 */
public interface SaasBillingProvider {

    SaasBillingProviderType type();

    SaasCheckoutResult createCheckout(SaasCheckoutCommand command);

    Optional<SaasSubscriptionSnapshot> findSubscription(
            String providerSubscriptionId
    );

    SaasCancellationResult cancel(String providerSubscriptionId);
}
