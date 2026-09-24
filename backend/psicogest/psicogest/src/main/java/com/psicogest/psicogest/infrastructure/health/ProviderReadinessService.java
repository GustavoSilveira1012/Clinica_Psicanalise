package com.psicogest.psicogest.infrastructure.health;

import com.psicogest.psicogest.infrastructure.notification.provider.NotificationProvider;
import com.psicogest.psicogest.infrastructure.payment.provider.PaymentProvider;
import com.psicogest.psicogest.infrastructure.payment.provider.webhook.PaymentWebhookAdapter;
import com.psicogest.psicogest.model.enums.NotificationChannel;
import com.psicogest.psicogest.service.fiscal.FailClosedNationalNfseClient;
import com.psicogest.psicogest.service.fiscal.NationalNfseClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reports whether the external integrations required for a production
 * deployment are actually wired. Empty registries and fail-closed clients are
 * intentionally unhealthy; a successful process boot is not proof that money,
 * messages or fiscal documents can be sent.
 */
@Service
public class ProviderReadinessService {

    private static final Set<NotificationChannel> REQUIRED_NOTIFICATION_CHANNELS =
            EnumSet.of(NotificationChannel.EMAIL, NotificationChannel.WHATSAPP);

    private final boolean requireRealProviders;
    private final List<PaymentProvider> paymentProviders;
    private final List<NotificationProvider> notificationProviders;
    private final List<PaymentWebhookAdapter> webhookAdapters;
    private final NationalNfseClient nationalNfseClient;

    public ProviderReadinessService(
            @Value("${app.integrations.require-real-providers:false}") boolean requireRealProviders,
            List<PaymentProvider> paymentProviders,
            List<NotificationProvider> notificationProviders,
            List<PaymentWebhookAdapter> webhookAdapters,
            NationalNfseClient nationalNfseClient
    ) {
        this.requireRealProviders = requireRealProviders;
        this.paymentProviders = List.copyOf(paymentProviders);
        this.notificationProviders = List.copyOf(notificationProviders);
        this.webhookAdapters = List.copyOf(webhookAdapters);
        this.nationalNfseClient = nationalNfseClient;
    }

    public boolean isReady() {
        if (!requireRealProviders) {
            return true;
        }
        Set<NotificationChannel> channels = notificationProviders.stream()
                .map(NotificationProvider::channel)
                .collect(Collectors.toUnmodifiableSet());
        Set<?> paymentTypes = paymentProviders.stream()
                .map(PaymentProvider::type)
                .collect(Collectors.toUnmodifiableSet());
        Set<?> webhookTypes = webhookAdapters.stream()
                .map(PaymentWebhookAdapter::getType)
                .collect(Collectors.toUnmodifiableSet());

        return !paymentTypes.isEmpty()
                && webhookTypes.containsAll(paymentTypes)
                && channels.containsAll(REQUIRED_NOTIFICATION_CHANNELS)
                && nationalNfseClient != null
                && !(nationalNfseClient instanceof FailClosedNationalNfseClient);
    }

    public Map<String, Object> status() {
        Set<NotificationChannel> channels = notificationProviders.stream()
                .map(NotificationProvider::channel)
                .collect(Collectors.toUnmodifiableSet());
        Set<?> paymentTypes = paymentProviders.stream()
                .map(PaymentProvider::type)
                .collect(Collectors.toUnmodifiableSet());
        Set<?> webhookTypes = webhookAdapters.stream()
                .map(PaymentWebhookAdapter::getType)
                .collect(Collectors.toUnmodifiableSet());

        return Map.of(
                "required", requireRealProviders,
                "ready", isReady(),
                "paymentProviders", paymentTypes.stream().map(Object::toString).sorted().toList(),
                "paymentWebhookAdapters", webhookTypes.stream().map(Object::toString).sorted().toList(),
                "notificationChannels", channels.stream().map(Enum::name).sorted().toList(),
                "nationalNfse", nationalNfseClient != null
                        && !(nationalNfseClient instanceof FailClosedNationalNfseClient)
        );
    }
}
