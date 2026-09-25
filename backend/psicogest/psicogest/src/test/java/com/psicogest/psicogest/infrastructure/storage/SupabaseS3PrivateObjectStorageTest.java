package com.psicogest.psicogest.infrastructure.storage;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SupabaseS3PrivateObjectStorageTest {

    private static final String BUCKET = "synthetic-test-exports";

    @Test
    void writesOnlyEncryptedContentToConfiguredPrivateBucket() {
        S3Client client = mock(S3Client.class);
        SupabaseS3PrivateObjectStorage storage = new SupabaseS3PrivateObjectStorage(client, BUCKET);
        String key = UUID.randomUUID() + "/" + UUID.randomUUID();
        byte[] ciphertext = new byte[]{1, 2, 3};

        storage.put(key, ciphertext, "application/octet-stream");

        verify(client).putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class));
    }

    @Test
    void rejectsUnsafeKeysAndAnythingNotMarkedAsCiphertext() {
        S3Client client = mock(S3Client.class);
        SupabaseS3PrivateObjectStorage storage = new SupabaseS3PrivateObjectStorage(client, BUCKET);

        assertThatThrownBy(() -> storage.put("../outside", new byte[]{1}, "application/octet-stream"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.put(UUID.randomUUID() + "/" + UUID.randomUUID(),
                "clinical plaintext".getBytes(StandardCharsets.UTF_8), "application/pdf"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(client, never()).putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class));
    }

    @Test
    void probesReachabilityWithDisposableMarkerButNeverClaimsUnverifiableProductionControls() throws Exception {
        S3Client client = mock(S3Client.class);
        @SuppressWarnings("unchecked")
        ResponseInputStream<GetObjectResponse> response = mock(ResponseInputStream.class);
        when(client.getObject(any(GetObjectRequest.class))).thenReturn(response);
        when(response.readAllBytes()).thenReturn("psicogest-storage-probe-v1".getBytes(StandardCharsets.US_ASCII));
        SupabaseS3PrivateObjectStorage storage = new SupabaseS3PrivateObjectStorage(client, BUCKET);

        PrivateObjectStorageHealth result = storage.healthCheck();

        assertThat(result.reachable()).isTrue();
        assertThat(result.privateAccessVerified()).isFalse();
        assertThat(result.atRestEncryptionVerified()).isFalse();
        assertThat(result.retentionPolicyVerified()).isFalse();
        assertThat(result.productionReady()).isFalse();
        verify(client).putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class));
        verify(client).deleteObject(any(DeleteObjectRequest.class));
        verify(response).close();
    }

    @Test
    void cachesProbeResultToAvoidWritingOnEveryReadinessRequest() throws Exception {
        S3Client client = mock(S3Client.class);
        @SuppressWarnings("unchecked")
        ResponseInputStream<GetObjectResponse> response = mock(ResponseInputStream.class);
        when(client.getObject(any(GetObjectRequest.class))).thenReturn(response);
        when(response.readAllBytes()).thenReturn("psicogest-storage-probe-v1".getBytes(StandardCharsets.US_ASCII));
        SupabaseS3PrivateObjectStorage storage = new SupabaseS3PrivateObjectStorage(client, BUCKET);

        storage.healthCheck();
        storage.healthCheck();

        verify(client).putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class));
        verify(client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void reportsUnreachableWhenProbeCannotReadBackMarker() throws Exception {
        S3Client client = mock(S3Client.class);
        @SuppressWarnings("unchecked")
        ResponseInputStream<GetObjectResponse> response = mock(ResponseInputStream.class);
        when(client.getObject(any(GetObjectRequest.class))).thenReturn(response);
        when(response.readAllBytes()).thenReturn(new byte[0]);
        SupabaseS3PrivateObjectStorage storage = new SupabaseS3PrivateObjectStorage(client, BUCKET);

        assertThat(storage.healthCheck().reachable()).isFalse();
        verify(client).deleteObject(any(DeleteObjectRequest.class));
    }
}
