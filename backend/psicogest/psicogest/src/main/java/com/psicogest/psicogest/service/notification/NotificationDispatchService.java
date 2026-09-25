package com.psicogest.psicogest.service.notification;

import com.psicogest.psicogest.infrastructure.notification.provider.NotificationProvider;
import com.psicogest.psicogest.infrastructure.notification.provider.NotificationProviderException;
import com.psicogest.psicogest.infrastructure.notification.provider.NotificationProviderRegistry;
import com.psicogest.psicogest.infrastructure.notification.provider.NotificationSendCommand;
import com.psicogest.psicogest.infrastructure.notification.provider.NotificationSendResult;
import com.psicogest.psicogest.model.enums.NotificationChannel;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Applies shared safety controls immediately before a provider call.
 * The financial entity must come from an authorized, tenant-scoped delivery
 * record, never directly from an untrusted HTTP request.
 */
@Service
public class NotificationDispatchService {

    private static final int MAX_DESTINATION_LENGTH = 512;
    private static final int MAX_SUBJECT_LENGTH = 998;
    private static final int MAX_BODY_LENGTH = 20_000;
    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 200;

    private final NotificationProviderRegistry providers;
    private final NotificationOutboundRateLimiter outboundRateLimiter;

    public NotificationDispatchService(
            NotificationProviderRegistry providers,
            NotificationOutboundRateLimiter outboundRateLimiter
    ) {
        this.providers = providers;
        this.outboundRateLimiter = outboundRateLimiter;
    }

    public NotificationSendResult send(
            UUID financialEntityId,
            NotificationChannel channel,
            NotificationSendCommand command
    ) {
        validate(financialEntityId, channel, command);

        // Resolve configuration before consuming a quota. The actual send is
        // impossible unless the distributed limit allows this tenant/channel.
        NotificationProvider provider = providers.get(channel);
        outboundRateLimiter.check(channel, financialEntityId);

        NotificationSendResult result = provider.send(command);
        if (result == null || result.state() == null) {
            throw new NotificationProviderException("Resposta inválida do provider de notificação");
        }
        return result;
    }

    private void validate(
            UUID financialEntityId,
            NotificationChannel channel,
            NotificationSendCommand command
    ) {
        if (financialEntityId == null || channel == null || command == null
                || command.deliveryId() == null
                || isBlankOrTooLong(command.destination(), MAX_DESTINATION_LENGTH)
                || hasLineBreak(command.destination())
                || command.body() == null || command.body().length() > MAX_BODY_LENGTH
                || isBlankOrTooLong(command.idempotencyKey(), MAX_IDEMPOTENCY_KEY_LENGTH)
                || hasLineBreak(command.idempotencyKey())
                || (command.subject() != null && (command.subject().length() > MAX_SUBJECT_LENGTH
                        || hasLineBreak(command.subject())))) {
            throw new IllegalArgumentException("Comando de notificação inválido");
        }
    }

    private boolean isBlankOrTooLong(String value, int maxLength) {
        return value == null || value.isBlank() || value.length() > maxLength;
    }

    private boolean hasLineBreak(String value) {
        return value != null && (value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0);
    }
}
