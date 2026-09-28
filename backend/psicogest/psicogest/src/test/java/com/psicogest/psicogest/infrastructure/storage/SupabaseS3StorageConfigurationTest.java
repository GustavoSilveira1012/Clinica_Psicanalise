package com.psicogest.psicogest.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SupabaseS3StorageConfigurationTest {

    private final SupabaseS3StorageConfiguration configuration = new SupabaseS3StorageConfiguration();

    @Test
    void acceptsTheDocumentedSupabaseStorageEndpointWithoutOpeningNetworkConnections() {
        try (var client = configuration.supabaseS3Client(
                "https://synthetic-project.storage.supabase.co/storage/v1/s3",
                "sa-east-1",
                "synthetic-access-key",
                "synthetic-secret-key")) {
            org.assertj.core.api.Assertions.assertThat(client).isNotNull();
        }
    }

    @Test
    void rejectsEndpointsThatCouldExfiltrateServerCredentials() {
        assertThatThrownBy(() -> configuration.supabaseS3Client(
                "https://attacker.example/storage/v1/s3", "sa-east-1", "key", "secret"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> configuration.supabaseS3Client(
                "https://synthetic-project.storage.supabase.co/storage/v1/s3?redirect=https://attacker.example",
                "sa-east-1", "key", "secret"))
                .isInstanceOf(IllegalStateException.class);
    }
}
