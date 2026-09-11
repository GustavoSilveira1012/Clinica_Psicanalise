package com.psicogest.psicogest.infrastructure.notification.provider;

import java.util.UUID;

public record NotificationSendCommand(
        UUID deliveryId,
        String destination,
        String subject,
        String body,
        String idempotencyKey
) {
}
