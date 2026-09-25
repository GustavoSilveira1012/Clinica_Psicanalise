package com.psicogest.psicogest.infrastructure.health;

import com.psicogest.psicogest.service.fiscal.FailClosedNationalNfseClient;
import com.psicogest.psicogest.infrastructure.storage.SecureClinicalExportStorage;
import com.psicogest.psicogest.infrastructure.storage.StoredExport;
import com.psicogest.psicogest.service.AuthActionMailProvider;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderReadinessServiceTest {

    @Test
    void localDevelopmentDoesNotPretendToValidateExternalProviders() {
        ProviderReadinessService service = new ProviderReadinessService(
                false, false, false, List.of(), List.of(), List.of(), new FailClosedNationalNfseClient(), List.of(), false,
                false, mailProvider(false));

        assertThat(service.isReady()).isTrue();
        assertThat(service.status())
                .containsEntry("required", false)
                .containsEntry("ready", true)
                .containsEntry("nationalNfse", false);
    }

    @Test
    void productionReadinessFailsClosedWhenAdaptersAreMissing() {
        ProviderReadinessService service = new ProviderReadinessService(
                true, true, true, List.of(), List.of(), List.of(), new FailClosedNationalNfseClient(), List.of(), false,
                false, mailProvider(false));

        assertThat(service.isReady()).isFalse();
        assertThat(service.status())
                .containsEntry("required", true)
                .containsEntry("ready", false);
    }

    @Test
    void productionReadinessRequiresDurableExportStorageEvenForClinicalOnlyScope() {
        ProviderReadinessService service = new ProviderReadinessService(
                false, false, false, List.of(), List.of(), List.of(), new FailClosedNationalNfseClient(), List.of(), true,
                false, mailProvider(false));

        assertThat(service.isReady()).isFalse();
        assertThat(service.status())
                .containsEntry("clinicalExportStorageRequired", true)
                .containsEntry("clinicalExportStorageReady", false);
    }

    @Test
    void productionReadinessAcceptsAnAvailablePrivateStorageAdapter() {
        SecureClinicalExportStorage storage = new SecureClinicalExportStorage() {
            @Override public boolean isAvailableForProduction() { return true; }
            @Override public StoredExport store(UUID tenantId, UUID id, byte[] content, String contentType) {
                return new StoredExport(id.toString(), "test", content.length);
            }
            @Override public InputStream open(UUID tenantId, String key) { return new ByteArrayInputStream(new byte[0]); }
            @Override public void delete(UUID tenantId, String key) { }
        };
        ProviderReadinessService service = new ProviderReadinessService(
                false, false, false, List.of(), List.of(), List.of(), new FailClosedNationalNfseClient(), List.of(storage), true,
                false, mailProvider(false));

        assertThat(service.isReady()).isTrue();
        assertThat(service.status()).containsEntry("clinicalExportStorageReady", true);
    }

    @Test
    void clinicalOnlyPilotDoesNotRequireExternalCommercialProviders() {
        ProviderReadinessService service = new ProviderReadinessService(
                false, false, false, List.of(), List.of(), List.of(),
                new FailClosedNationalNfseClient(), List.of(), false, false, mailProvider(false));

        assertThat(service.isReady()).isTrue();
        assertThat(service.status())
                .containsEntry("required", false)
                .containsEntry("paymentProvidersRequired", false)
                .containsEntry("notificationProvidersRequired", false)
                .containsEntry("nationalNfseRequired", false);
    }

    @Test
    void eachEnabledIntegrationIsIndependentlyRequired() {
        ProviderReadinessService service = new ProviderReadinessService(
                true, false, false, List.of(), List.of(), List.of(),
                new FailClosedNationalNfseClient(), List.of(), false, false, mailProvider(false));

        assertThat(service.isReady()).isFalse();
        assertThat(service.status()).containsEntry("paymentProvidersRequired", true);
    }

    @Test
    void readinessRequiresConfiguredMailOnlyWhenExplicitlyEnabled() {
        ProviderReadinessService service = new ProviderReadinessService(
                false, false, false, List.of(), List.of(), List.of(),
                new FailClosedNationalNfseClient(), List.of(), false, true, mailProvider(false));

        assertThat(service.isReady()).isFalse();
        assertThat(service.status())
                .containsEntry("accountActionMailRequired", true)
                .containsEntry("accountActionMailReady", false);
    }

    private static AuthActionMailProvider mailProvider(boolean available) {
        return new AuthActionMailProvider() {
            @Override public boolean isAvailable() { return available; }
            @Override public void send(String recipient, String subject, String body) {
                throw new UnsupportedOperationException("Test provider does not send email");
            }
        };
    }
}
