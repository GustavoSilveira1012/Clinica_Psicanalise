package com.psicogest.psicogest.infrastructure.notification.provider;

public record NotificationSendResult(
        String providerMessageId,
        ProviderDeliveryState state
) {
}
