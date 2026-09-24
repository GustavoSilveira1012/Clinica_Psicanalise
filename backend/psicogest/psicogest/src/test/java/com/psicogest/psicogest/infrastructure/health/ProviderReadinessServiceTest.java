package com.psicogest.psicogest.infrastructure.health;

import com.psicogest.psicogest.service.fiscal.FailClosedNationalNfseClient;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderReadinessServiceTest {

    @Test
    void localDevelopmentDoesNotPretendToValidateExternalProviders() {
        ProviderReadinessService service = new ProviderReadinessService(
                false, List.of(), List.of(), List.of(), new FailClosedNationalNfseClient());

        assertThat(service.isReady()).isTrue();
        assertThat(service.status())
                .containsEntry("required", false)
                .containsEntry("ready", true)
                .containsEntry("nationalNfse", false);
    }

    @Test
    void productionReadinessFailsClosedWhenAdaptersAreMissing() {
        ProviderReadinessService service = new ProviderReadinessService(
                true, List.of(), List.of(), List.of(), new FailClosedNationalNfseClient());

        assertThat(service.isReady()).isFalse();
        assertThat(service.status())
                .containsEntry("required", true)
                .containsEntry("ready", false);
    }
}
