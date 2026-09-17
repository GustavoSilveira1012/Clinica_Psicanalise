package com.psicogest.psicogest.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationDeliveryResponse(
        UUID id,
        Instant createdAt,
        String recipientLabel,
        String channel,
        String eventType,
        String status,
        String providerMessageId
) {
}
