package com.psicogest.psicogest.service.notification;

import com.psicogest.psicogest.infrastructure.notification.provider.NotificationProvider;
import com.psicogest.psicogest.infrastructure.notification.provider.NotificationProviderException;
import com.psicogest.psicogest.infrastructure.notification.provider.NotificationProviderRegistry;
import com.psicogest.psicogest.infrastructure.notification.provider.NotificationSendCommand;
import com.psicogest.psicogest.infrastructure.notification.provider.NotificationSendResult;
import com.psicogest.psicogest.infrastructure.notification.provider.ProviderDeliveryState;
import com.psicogest.psicogest.model.enums.NotificationChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationDispatchServiceTest {

    private final NotificationProviderRegistry providers = mock(NotificationProviderRegistry.class);
    private final NotificationOutboundRateLimiter rateLimiter = mock(NotificationOutboundRateLimiter.class);
    private final NotificationProvider provider = mock(NotificationProvider.class);
    private final NotificationDispatchService service = new NotificationDispatchService(providers, rateLimiter);
    private final UUID financialEntityId = UUID.randomUUID();
    private final NotificationSendCommand command = new NotificationSendCommand(
            UUID.randomUUID(), "patient@example.invalid", "Lembrete", "Mensagem sintética", "delivery-key-1");

    @BeforeEach
    void configureProvider() {
        when(providers.get(NotificationChannel.EMAIL)).thenReturn(provider);
    }

    @Test
    void appliesTenantChannelRateLimitBeforeCallingProvider() {
        NotificationSendResult expected = new NotificationSendResult("synthetic-message-id", ProviderDeliveryState.SENT);
        when(provider.send(command)).thenReturn(expected);

        assertThat(service.send(financialEntityId, NotificationChannel.EMAIL, command)).isEqualTo(expected);

        InOrder order = inOrder(rateLimiter, provider);
        order.verify(rateLimiter).check(NotificationChannel.EMAIL, financialEntityId);
        order.verify(provider).send(command);
    }

    @Test
    void doesNotCallProviderWhenOutboundQuotaIsExceeded() {
        org.mockito.Mockito.doThrow(new com.psicogest.psicogest.exception.RateLimitExceededException())
                .when(rateLimiter).check(NotificationChannel.EMAIL, financialEntityId);

        assertThatThrownBy(() -> service.send(financialEntityId, NotificationChannel.EMAIL, command))
                .isInstanceOf(com.psicogest.psicogest.exception.RateLimitExceededException.class);
        verify(provider, never()).send(command);
    }

    @Test
    void rejectsHeaderInjectionAndOversizedPayloadBeforeProviderLookup() {
        NotificationSendCommand invalid = new NotificationSendCommand(
                command.deliveryId(), "victim@example.invalid\r\nBcc: attacker@example.invalid",
                command.subject(), command.body(), command.idempotencyKey());

        assertThatThrownBy(() -> service.send(financialEntityId, NotificationChannel.EMAIL, invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Comando de notificação inválido");
        verify(providers, never()).get(NotificationChannel.EMAIL);
        verify(rateLimiter, never()).check(NotificationChannel.EMAIL, financialEntityId);
    }

    @Test
    void rejectsUnconfiguredChannelWithoutConsumingQuota() {
        when(providers.get(NotificationChannel.WHATSAPP))
                .thenThrow(new NotificationProviderException("Canal não configurado"));

        assertThatThrownBy(() -> service.send(financialEntityId, NotificationChannel.WHATSAPP, command))
                .isInstanceOf(NotificationProviderException.class);
        verify(rateLimiter, never()).check(NotificationChannel.WHATSAPP, financialEntityId);
    }

    @Test
    void rejectsInvalidProviderResponse() {
        when(provider.send(command)).thenReturn(null);

        assertThatThrownBy(() -> service.send(financialEntityId, NotificationChannel.EMAIL, command))
                .isInstanceOf(NotificationProviderException.class)
                .hasMessage("Resposta inválida do provider de notificação");
    }
}
