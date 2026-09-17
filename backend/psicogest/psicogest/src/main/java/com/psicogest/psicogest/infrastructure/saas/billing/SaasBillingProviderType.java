package com.psicogest.psicogest.infrastructure.saas.billing;

/** Gateways que podem cobrar a assinatura do próprio PsicoGest. */
public enum SaasBillingProviderType {
    ASAAS,
    MERCADO_PAGO,
    PAGARME,
    STRIPE
}
