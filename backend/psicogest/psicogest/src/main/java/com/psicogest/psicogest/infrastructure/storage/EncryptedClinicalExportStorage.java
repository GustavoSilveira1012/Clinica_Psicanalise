package com.psicogest.psicogest.infrastructure.storage;

import com.psicogest.psicogest.security.crypto.ApplicationEncryptionService;
import com.psicogest.psicogest.security.crypto.EncryptedEnvelope;
import com.psicogest.psicogest.security.crypto.EncryptionContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * Encrypts clinical export bytes at the application boundary before delegating
 * to a provider. The production readiness probe remains false unless the
 * provider proves reachability, private access, encryption, and retention.
 */
@Component
@Profile("production")
@ConditionalOnProperty(name = "app.export-storage.type", havingValue = "private-object")
@ConditionalOnBean(PrivateObjectStorage.class)
public class EncryptedClinicalExportStorage implements SecureClinicalExportStorage {

    private static final int ENVELOPE_MAGIC = 0x50474531; // PGE1
    private static final int MAX_ENVELOPE_FIELD_BYTES = 64 * 1024 * 1024;
    private static final int MAX_PLAINTEXT_BYTES = 40 * 1024 * 1024;
    private static final String OBJECT_CONTENT_TYPE = "application/octet-stream";

    private final PrivateObjectStorage objectStorage;
    private final ApplicationEncryptionService encryption;

    public EncryptedClinicalExportStorage(
            PrivateObjectStorage objectStorage,
            ApplicationEncryptionService encryption
    ) {
        this.objectStorage = objectStorage;
        this.encryption = encryption;
    }

    @Override
    public boolean isAvailableForProduction() {
        try {
            PrivateObjectStorageHealth health = objectStorage.healthCheck();
            return health != null && health.productionReady();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    @Override
    public StoredExport store(UUID financialEntityId, UUID exportId, byte[] content, String contentType) {
        if (financialEntityId == null || exportId == null || content == null || content.length == 0) {
            throw new IllegalArgumentException("Tenant, export e conteúdo são obrigatórios");
        }
        if (content.length > MAX_PLAINTEXT_BYTES) {
            throw new IllegalArgumentException("Export excede o limite seguro de armazenamento");
        }

        String storageKey = storageKey(financialEntityId, exportId);
        String encoded = Base64.getEncoder().encodeToString(content);
        EncryptedEnvelope envelope = encryption.encrypt(encoded, context(financialEntityId, exportId));
        byte[] serialized = serialize(envelope);
        try {
            objectStorage.put(storageKey, serialized, OBJECT_CONTENT_TYPE);
            return new StoredExport(storageKey, sha256(content), content.length);
        } finally {
            Arrays.fill(serialized, (byte) 0);
        }
    }

    @Override
    public InputStream open(UUID financialEntityId, String storageKey) {
        UUID exportId = exportIdForTenant(financialEntityId, storageKey);
        try (InputStream stored = objectStorage.get(storageKey)) {
            EncryptedEnvelope envelope = deserialize(stored);
            byte[] plaintext = Base64.getDecoder().decode(
                    encryption.decrypt(envelope, context(financialEntityId, exportId)));
            if (plaintext.length > MAX_PLAINTEXT_BYTES) {
                Arrays.fill(plaintext, (byte) 0);
                throw new IllegalStateException("Export excede o limite seguro de leitura");
            }
            return new WipingInputStream(plaintext);
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("Não foi possível recuperar o export clínico", exception);
        }
    }

    @Override
    public void delete(UUID financialEntityId, String storageKey) {
        exportIdForTenant(financialEntityId, storageKey);
        objectStorage.delete(storageKey);
    }

    private EncryptionContext context(UUID financialEntityId, UUID exportId) {
        return new EncryptionContext(
                "CLINICAL_EXPORT",
                exportId.toString(),
                "content",
                Map.of("financialEntityId", financialEntityId.toString())
        );
    }

    private String storageKey(UUID financialEntityId, UUID exportId) {
        return financialEntityId + "/" + exportId;
    }

    private UUID exportIdForTenant(UUID financialEntityId, String storageKey) {
        if (financialEntityId == null || storageKey == null) {
            throw new IllegalArgumentException("Tenant e chave do export são obrigatórios");
        }
        String prefix = financialEntityId + "/";
        if (!storageKey.startsWith(prefix) || storageKey.length() != prefix.length() + 36) {
            throw new IllegalArgumentException("Chave de export não pertence ao tenant informado");
        }
        try {
            UUID exportId = UUID.fromString(storageKey.substring(prefix.length()));
            if (!storageKey.equals(storageKey(financialEntityId, exportId))) {
                throw new IllegalArgumentException("Chave de export inválida");
            }
            return exportId;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Chave de export inválida");
        }
    }

    private byte[] serialize(EncryptedEnvelope envelope) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(ENVELOPE_MAGIC);
                output.writeInt(envelope.cryptoVersion());
                output.writeUTF(envelope.algorithm());
                output.writeUTF(envelope.keyId());
                writeField(output, envelope.wrappedDataKey());
                writeField(output, envelope.iv());
                writeField(output, envelope.ciphertext());
            }
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Falha ao preparar export cifrado", exception);
        }
    }

    private EncryptedEnvelope deserialize(InputStream stored) throws IOException {
        try (DataInputStream input = new DataInputStream(stored)) {
            if (input.readInt() != ENVELOPE_MAGIC) {
                throw new IOException("Formato de envelope inválido");
            }
            int version = input.readInt();
            String algorithm = input.readUTF();
            String keyId = input.readUTF();
            byte[] wrappedKey = readField(input);
            byte[] iv = readField(input);
            byte[] ciphertext = readField(input);
            if (input.read() != -1) {
                throw new IOException("Envelope possui dados extras");
            }
            return new EncryptedEnvelope(version, algorithm, keyId, wrappedKey, iv, ciphertext);
        }
    }

    private void writeField(DataOutputStream output, byte[] value) throws IOException {
        if (value.length > MAX_ENVELOPE_FIELD_BYTES) {
            throw new IllegalArgumentException("Envelope excede o limite seguro");
        }
        output.writeInt(value.length);
        output.write(value);
    }

    private byte[] readField(DataInputStream input) throws IOException {
        int length = input.readInt();
        if (length < 0 || length > MAX_ENVELOPE_FIELD_BYTES) {
            throw new IOException("Campo do envelope excede o limite seguro");
        }
        byte[] value = input.readNBytes(length);
        if (value.length != length) {
            throw new IOException("Envelope truncado");
        }
        return value;
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (Exception exception) {
            throw new IllegalStateException("Falha ao calcular integridade do export", exception);
        }
    }

    private static final class WipingInputStream extends ByteArrayInputStream {
        private WipingInputStream(byte[] plaintext) {
            super(plaintext);
        }

        @Override
        public void close() throws IOException {
            Arrays.fill(buf, (byte) 0);
            super.close();
        }
    }
}
