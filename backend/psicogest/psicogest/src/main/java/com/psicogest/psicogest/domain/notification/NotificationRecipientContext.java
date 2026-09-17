package com.psicogest.psicogest.domain.notification;

import java.util.UUID;

/**
 * Dados mínimos usados pelo motor de elegibilidade. O destino deve existir
 * apenas durante o processamento e nunca deve ser colocado em logs.
 */
public record NotificationRecipientContext(
        UUID financialEntityId,
        String destination,
        boolean preferenceEnabled,
        boolean required
) implements NotificationRecipient {
}
