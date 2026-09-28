package com.psicogest.psicogest.infrastructure.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;
import java.time.Duration;

/** Server-only credentials; this adapter is deliberately production-profile gated. */
@Configuration
@Profile("production")
@ConditionalOnProperty(name = "app.export-storage.type", havingValue = "supabase-s3")
public class SupabaseS3StorageConfiguration {

    @Bean(destroyMethod = "close")
    S3Client supabaseS3Client(
            @Value("${app.export-storage.supabase.endpoint}") String endpoint,
            @Value("${app.export-storage.supabase.region}") String region,
            @Value("${app.export-storage.supabase.access-key}") String accessKey,
            @Value("${app.export-storage.supabase.secret-key}") String secretKey
    ) {
        URI endpointUri = URI.create(endpoint);
        String host = endpointUri.getHost() == null ? "" : endpointUri.getHost().toLowerCase(java.util.Locale.ROOT);
        if (!"https".equalsIgnoreCase(endpointUri.getScheme())
                || !host.matches("[a-z0-9-]+\\.storage\\.supabase\\.co")
                || !"/storage/v1/s3".equals(endpointUri.getPath())
                || endpointUri.getUserInfo() != null
                || endpointUri.getQuery() != null
                || endpointUri.getFragment() != null
                || (endpointUri.getPort() != -1 && endpointUri.getPort() != 443)) {
            throw new IllegalStateException("O endpoint S3 deve ser o endpoint oficial Supabase Storage via HTTPS");
        }
        if (accessKey.isBlank() || secretKey.isBlank() || region.isBlank()) {
            throw new IllegalStateException("Credenciais e região do storage S3 são obrigatórias");
        }

        return S3Client.builder()
                .endpointOverride(endpointUri)
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallAttemptTimeout(Duration.ofSeconds(5))
                        .apiCallTimeout(Duration.ofSeconds(15))
                        .build())
                .build();
    }
}
