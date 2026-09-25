package com.psicogest.psicogest.infrastructure.health;

import com.psicogest.psicogest.infrastructure.notification.provider.NotificationProvider;
import com.psicogest.psicogest.infrastructure.payment.provider.PaymentProvider;
import com.psicogest.psicogest.infrastructure.payment.provider.webhook.PaymentWebhookAdapter;
import com.psicogest.psicogest.infrastructure.storage.SecureClinicalExportStorage;
import com.psicogest.psicogest.model.enums.NotificationChannel;
import com.psicogest.psicogest.service.fiscal.FailClosedNationalNfseClient;
import com.psicogest.psicogest.service.fiscal.NationalNfseClient;
import com.psicogest.psicogest.service.AuthActionMailProvider;
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

    private final boolean requirePaymentProviders;
    private final boolean requireNotificationProviders;
    private final boolean requireNationalNfse;
    private final boolean requireClinicalExportStorage;
    private final boolean requireAuthActionMail;
    private final AuthActionMailProvider authActionMailProvider;
    private final List<PaymentProvider> paymentProviders;
    private final List<NotificationProvider> notificationProviders;
    private final List<PaymentWebhookAdapter> webhookAdapters;
    private final NationalNfseClient nationalNfseClient;
    private final List<SecureClinicalExportStorage> clinicalExportStorages;

    public ProviderReadinessService(
            @Value("${app.integrations.require-payment-providers:${app.integrations.require-real-providers:false}}") boolean requirePaymentProviders,
            @Value("${app.integrations.require-notification-providers:${app.integrations.require-real-providers:false}}") boolean requireNotificationProviders,
            @Value("${app.integrations.require-national-nfse:${app.integrations.require-real-providers:false}}") boolean requireNationalNfse,
            List<PaymentProvider> paymentProviders,
            List<NotificationProvider> notificationProviders,
            List<PaymentWebhookAdapter> webhookAdapters,
            NationalNfseClient nationalNfseClient,
            List<SecureClinicalExportStorage> clinicalExportStorages,
            @Value("${app.export-storage.required-for-readiness:false}") boolean requireClinicalExportStorage,
            @Value("${app.auth-actions.mail.required-for-readiness:false}") boolean requireAuthActionMail,
            AuthActionMailProvider authActionMailProvider
    ) {
        this.requirePaymentProviders = requirePaymentProviders;
        this.requireNotificationProviders = requireNotificationProviders;
        this.requireNationalNfse = requireNationalNfse;
        this.paymentProviders = List.copyOf(paymentProviders);
        this.notificationProviders = List.copyOf(notificationProviders);
        this.webhookAdapters = List.copyOf(webhookAdapters);
        this.nationalNfseClient = nationalNfseClient;
        this.clinicalExportStorages = List.copyOf(clinicalExportStorages);
        this.requireClinicalExportStorage = requireClinicalExportStorage;
        this.requireAuthActionMail = requireAuthActionMail;
        this.authActionMailProvider = authActionMailProvider;
    }

    public boolean isReady() {
        if (requireClinicalExportStorage && !clinicalExportStorageReady()) {
            return false;
        }
        if (requireAuthActionMail && !authActionMailProvider.isAvailable()) {
            return false;
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

        boolean paymentsReady = !requirePaymentProviders
                || (!paymentTypes.isEmpty() && webhookTypes.containsAll(paymentTypes));
        boolean notificationsReady = !requireNotificationProviders
                || channels.containsAll(REQUIRED_NOTIFICATION_CHANNELS);
        boolean nfseReady = !requireNationalNfse
                || (nationalNfseClient != null
                && !(nationalNfseClient instanceof FailClosedNationalNfseClient));

        return paymentsReady && notificationsReady && nfseReady;
    }

    private boolean clinicalExportStorageReady() {
        return clinicalExportStorages.stream()
                .anyMatch(SecureClinicalExportStorage::isAvailableForProduction);
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

        return Map.ofEntries(
                Map.entry("required", requirePaymentProviders || requireNotificationProviders
                        || requireNationalNfse || requireClinicalExportStorage || requireAuthActionMail),
                Map.entry("ready", isReady()),
                Map.entry("paymentProvidersRequired", requirePaymentProviders),
                Map.entry("notificationProvidersRequired", requireNotificationProviders),
                Map.entry("nationalNfseRequired", requireNationalNfse),
                Map.entry("clinicalExportStorageRequired", requireClinicalExportStorage),
                Map.entry("clinicalExportStorageReady", clinicalExportStorageReady()),
                Map.entry("accountActionMailRequired", requireAuthActionMail),
                Map.entry("accountActionMailReady", authActionMailProvider.isAvailable()),
                Map.entry("paymentProviders", paymentTypes.stream().map(Object::toString).sorted().toList()),
                Map.entry("paymentWebhookAdapters", webhookTypes.stream().map(Object::toString).sorted().toList()),
                Map.entry("notificationChannels", channels.stream().map(Enum::name).sorted().toList()),
                Map.entry("nationalNfse", nationalNfseClient != null
                        && !(nationalNfseClient instanceof FailClosedNationalNfseClient))
        );
    }
}
