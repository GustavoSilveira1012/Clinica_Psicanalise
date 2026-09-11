package com.psicogest.psicogest.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSubscriptionMandateRequest(
        @NotBlank String provider,
        String providerCustomerReference,
        @NotBlank String providerPaymentMethodReference
) {
}
