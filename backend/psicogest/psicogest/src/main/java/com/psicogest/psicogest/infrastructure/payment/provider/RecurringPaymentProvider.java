package com.psicogest.psicogest.infrastructure.payment.provider;

public interface RecurringPaymentProvider {

    PaymentProviderType type();

    RecurringChargeResult charge(RecurringChargeCommand command);
}
