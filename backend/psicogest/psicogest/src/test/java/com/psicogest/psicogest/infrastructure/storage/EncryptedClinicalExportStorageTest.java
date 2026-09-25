package com.psicogest.psicogest.infrastructure.storage;

import com.psicogest.psicogest.security.crypto.ApplicationEncryptionService;
import com.psicogest.psicogest.security.crypto.EncryptedEnvelope;
import com.psicogest.psicogest.security.crypto.EncryptionContext;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EncryptedClinicalExportStorageTest {

    private static final UUID TENANT = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_TENANT = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID EXPORT = UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final byte[] ENCRYPTED_MARKER = "ciphertext-only".getBytes(StandardCharsets.UTF_8);

    @Test
    void encryptsBeforeProviderWriteAndBindsEncryptionContextToTenant() {
        InMemoryObjectStorage provider = new InMemoryObjectStorage();
        ApplicationEncryptionService encryption = mock(ApplicationEncryptionService.class);
        EncryptedClinicalExportStorage storage = new EncryptedClinicalExportStorage(provider, encryption);
        byte[] content = "synthetic-clinical-export".getBytes(StandardCharsets.UTF_8);
        when(encryption.encrypt(anyString(), any(EncryptionContext.class))).thenReturn(envelope());

        StoredExport stored = storage.store(TENANT, EXPORT, content, "application/pdf");

        assertThat(stored.storageKey()).isEqualTo(TENANT + "/" + EXPORT);
        assertThat(stored.size()).isEqualTo("synthetic-clinical-export".length());
        assertThat(new String(provider.objects.get(stored.storageKey()), StandardCharsets.ISO_8859_1))
                .doesNotContain("synthetic-clinical-export");
        assertThat(provider.contentTypes.get(stored.storageKey())).isEqualTo("application/octet-stream");
        org.mockito.ArgumentCaptor<EncryptionContext> context = org.mockito.ArgumentCaptor.forClass(EncryptionContext.class);
        verify(encryption).encrypt(anyString(), context.capture());
        assertThat(context.getValue().attributes()).containsEntry("financialEntityId", TENANT.toString());
        assertThat(context.getValue().resourceId()).isEqualTo(EXPORT.toString());
    }

    @Test
    void refusesCrossTenantReadAndDeleteBeforeCallingProvider() {
        PrivateObjectStorage provider = mock(PrivateObjectStorage.class);
        EncryptedClinicalExportStorage storage = new EncryptedClinicalExportStorage(
                provider, mock(ApplicationEncryptionService.class));

        assertThatThrownBy(() -> storage.open(OTHER_TENANT, TENANT + "/" + EXPORT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.delete(OTHER_TENANT, TENANT + "/" + EXPORT))
                .isInstanceOf(IllegalArgumentException.class);
        verify(provider, never()).get(anyString());
        verify(provider, never()).delete(anyString());
    }

    @Test
    void decryptsOnlyWithMatchingTenantContextAndWipesReturnedBufferOnClose() throws Exception {
        InMemoryObjectStorage provider = new InMemoryObjectStorage();
        ApplicationEncryptionService encryption = mock(ApplicationEncryptionService.class);
        EncryptedClinicalExportStorage storage = new EncryptedClinicalExportStorage(provider, encryption);
        String key = TENANT + "/" + EXPORT;
        provider.put(key, serialize(envelope()), "application/octet-stream");
        byte[] original = "private bytes".getBytes(StandardCharsets.UTF_8);
        when(encryption.decrypt(any(EncryptedEnvelope.class), any(EncryptionContext.class)))
                .thenReturn(Base64.getEncoder().encodeToString(original));

        InputStream input = storage.open(TENANT, key);
        assertThat(input.readAllBytes()).containsExactly(original);
        input.close();
        org.mockito.ArgumentCaptor<EncryptionContext> context = org.mockito.ArgumentCaptor.forClass(EncryptionContext.class);
        verify(encryption).decrypt(any(EncryptedEnvelope.class), context.capture());
        assertThat(context.getValue().attributes()).containsEntry("financialEntityId", TENANT.toString());
    }

    @Test
    void productionReadinessRequiresObservedProviderControls() {
        PrivateObjectStorage provider = mock(PrivateObjectStorage.class);
        EncryptedClinicalExportStorage storage = new EncryptedClinicalExportStorage(
                provider, mock(ApplicationEncryptionService.class));
        when(provider.healthCheck()).thenReturn(new PrivateObjectStorageHealth(true, true, true, false));
        assertThat(storage.isAvailableForProduction()).isFalse();
        when(provider.healthCheck()).thenReturn(new PrivateObjectStorageHealth(true, true, true, true));
        assertThat(storage.isAvailableForProduction()).isTrue();
    }

    private EncryptedEnvelope envelope() {
        return new EncryptedEnvelope(1, "AES-256-GCM", "test-key", new byte[]{1, 2},
                new byte[12], ENCRYPTED_MARKER);
    }

    private byte[] serialize(EncryptedEnvelope envelope) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(0x50474531);
            output.writeInt(envelope.cryptoVersion());
            output.writeUTF(envelope.algorithm());
            output.writeUTF(envelope.keyId());
            write(output, envelope.wrappedDataKey());
            write(output, envelope.iv());
            write(output, envelope.ciphertext());
        }
        return bytes.toByteArray();
    }

    private void write(DataOutputStream output, byte[] value) throws Exception {
        output.writeInt(value.length);
        output.write(value);
    }

    private static final class InMemoryObjectStorage implements PrivateObjectStorage {
        private final Map<String, byte[]> objects = new HashMap<>();
        private final Map<String, String> contentTypes = new HashMap<>();

        @Override
        public void put(String key, byte[] ciphertext, String contentType) {
            objects.put(key, ciphertext.clone());
            contentTypes.put(key, contentType);
        }

        @Override
        public InputStream get(String key) {
            byte[] data = objects.get(key);
            return data == null ? new ByteArrayInputStream(new byte[0]) : new ByteArrayInputStream(data.clone());
        }

        @Override
        public void delete(String key) {
            byte[] removed = objects.remove(key);
            if (removed != null) Arrays.fill(removed, (byte) 0);
            contentTypes.remove(key);
        }

        @Override
        public PrivateObjectStorageHealth healthCheck() {
            return new PrivateObjectStorageHealth(false, false, false, false);
        }
    }
}
