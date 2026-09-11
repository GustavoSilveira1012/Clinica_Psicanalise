package com.psicogest.psicogest.infrastructure.payment.provider;

public record RecurringChargeResult(
        RecurringChargeStatus status,
        String providerTransactionId,
        String failureCode
) {
}
