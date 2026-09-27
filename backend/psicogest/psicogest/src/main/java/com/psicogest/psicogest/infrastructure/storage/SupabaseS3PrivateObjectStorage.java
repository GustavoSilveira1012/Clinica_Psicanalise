package com.psicogest.psicogest.infrastructure.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Supabase's S3 endpoint is enabled only for synthetic-data testing. Payloads
 * are encrypted by {@link EncryptedClinicalExportStorage} before reaching this
 * adapter. The provider API cannot prove bucket privacy, encryption-at-rest,
 * or retention, so this adapter can never satisfy the production gate.
 */
@Component
@Profile("production")
@ConditionalOnProperty(name = "app.export-storage.type", havingValue = "supabase-s3")
public class SupabaseS3PrivateObjectStorage implements PrivateObjectStorage {

    private static final Duration HEALTH_CACHE_TTL = Duration.ofMinutes(5);
    private static final byte[] HEALTH_MARKER = "psicogest-storage-probe-v1".getBytes(StandardCharsets.US_ASCII);

    private final S3Client client;
    private final String bucket;
    private volatile CachedHealth cachedHealth;

    public SupabaseS3PrivateObjectStorage(
            S3Client client,
            @Value("${app.export-storage.supabase.bucket}") String bucket
    ) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("Bucket privado de storage não configurado");
        }
        this.client = client;
        this.bucket = bucket;
    }

    @Override
    public void put(String key, byte[] ciphertext, String contentType) {
        requireSafeKey(key);
        if (ciphertext == null || ciphertext.length == 0 || contentType == null
                || !"application/octet-stream".equals(contentType)) {
            throw new IllegalArgumentException("Somente envelopes cifrados podem ser enviados ao storage");
        }
        try {
            client.putObject(PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromBytes(ciphertext));
        } catch (RuntimeException exception) {
            throw storageFailure();
        }
    }

    @Override
    public InputStream get(String key) {
        requireSafeKey(key);
        try {
            ResponseInputStream<GetObjectResponse> response = client.getObject(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
            return response;
        } catch (RuntimeException exception) {
            throw storageFailure();
        }
    }

    @Override
    public void delete(String key) {
        requireSafeKey(key);
        try {
            client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
        } catch (RuntimeException exception) {
            throw storageFailure();
        }
    }

    @Override
    public PrivateObjectStorageHealth healthCheck() {
        CachedHealth current = cachedHealth;
        Instant now = Instant.now();
        if (current != null && current.checkedAt().plus(HEALTH_CACHE_TTL).isAfter(now)) {
            return current.health();
        }
        synchronized (this) {
            current = cachedHealth;
            now = Instant.now();
            if (current != null && current.checkedAt().plus(HEALTH_CACHE_TTL).isAfter(now)) {
                return current.health();
            }
            PrivateObjectStorageHealth health = runSyntheticProbe();
            cachedHealth = new CachedHealth(Instant.now(), health);
            return health;
        }
    }

    private PrivateObjectStorageHealth runSyntheticProbe() {
        String key = "__health/" + UUID.randomUUID();
        boolean reachable = false;
        try {
            client.putObject(PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType("application/octet-stream")
                            .build(),
                    RequestBody.fromBytes(HEALTH_MARKER));
            try (InputStream input = client.getObject(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build())) {
                reachable = java.util.Arrays.equals(input.readAllBytes(), HEALTH_MARKER);
            }
        } catch (RuntimeException | java.io.IOException ignored) {
            reachable = false;
        } finally {
            try {
                client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build());
            } catch (RuntimeException ignored) {
                // A failed cleanup keeps production readiness closed.
                reachable = false;
            }
        }
        // Supabase S3 cannot verify private ACL, server-side encryption, or retention.
        return new PrivateObjectStorageHealth(reachable, false, false, false);
    }

    private void requireSafeKey(String key) {
        if (key == null || key.isBlank() || key.startsWith("/") || key.contains("..")
                || key.indexOf('\\') >= 0 || key.length() > 200
                || !(key.matches("[0-9a-fA-F-]{36}/[0-9a-fA-F-]{36}")
                || key.matches("__health/[0-9a-fA-F-]{36}"))) {
            throw new IllegalArgumentException("Chave de objeto inválida");
        }
    }

    private IllegalStateException storageFailure() {
        // Do not expose endpoint, bucket credentials, object keys, or provider response bodies.
        return new IllegalStateException("Operação de storage indisponível");
    }

    private record CachedHealth(Instant checkedAt, PrivateObjectStorageHealth health) {}
}
