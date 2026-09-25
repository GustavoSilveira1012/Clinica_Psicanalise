package com.psicogest.psicogest.infrastructure.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.UUID;

/**
 * Implementação local para DEV
 * 
 * Armazena exports em sistema de arquivos local
 * Produção: usar S3, Azure Blob ou MinIO
 */
@Slf4j
@Component
@Profile("!production")
@ConditionalOnProperty(
        name = "app.export-storage.type",
        havingValue = "local",
        matchIfMissing = true
)
public class LocalClinicalExportStorage implements SecureClinicalExportStorage {

    private final Path exportDir;

    public LocalClinicalExportStorage() {
        try {
            String exportPath = System.getProperty(
                    "app.export-storage.local.path",
                    "clinical-exports"
            );
            this.exportDir = Paths.get(exportPath);
            Files.createDirectories(this.exportDir);
            log.info("LocalClinicalExportStorage inicializado: dir={}", 
                    this.exportDir.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Falha ao inicializar diretório de exports", e);
        }
    }

    @Override
    public StoredExport store(UUID financialEntityId, UUID exportId, byte[] content, String contentType) {
        try {
            // Gera SHA-256 do conteúdo
            String sha256 = calculateSha256(content);
            
            // Storage key: exportId
            String storageKey = exportId.toString();
            Path tenantDir = exportDir.resolve(financialEntityId.toString()).normalize();
            if (!tenantDir.startsWith(exportDir.normalize())) {
                throw new IllegalArgumentException("Tenant inválido para armazenamento de exportação");
            }
            Files.createDirectories(tenantDir);
            Path filePath = tenantDir.resolve(storageKey).normalize();
            if (!filePath.startsWith(tenantDir)) {
                throw new IllegalArgumentException("Chave inválida para armazenamento de exportação");
            }

            // Escreve arquivo
            Files.write(filePath, content);

            log.info("Export armazenado: storageKey={}, size={}, sha256={}",
                    storageKey, content.length, sha256);

            return new StoredExport(storageKey, sha256, content.length);
        } catch (IOException e) {
            throw new RuntimeException("Falha ao armazenar export", e);
        }
    }

    @Override
    public InputStream open(UUID financialEntityId, String storageKey) {
        try {
            Path filePath = tenantPath(financialEntityId, storageKey);
            
            if (!Files.exists(filePath)) {
                throw new FileNotFoundException("Export não encontrado: " + storageKey);
            }

            log.info("Export aberto para download: storageKey={}", storageKey);
            return new FileInputStream(filePath.toFile());
        } catch (IOException e) {
            throw new RuntimeException("Falha ao abrir export", e);
        }
    }

    @Override
    public void delete(UUID financialEntityId, String storageKey) {
        try {
            Path filePath = tenantPath(financialEntityId, storageKey);
            
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Export deletado: storageKey={}", storageKey);
            }
        } catch (IOException e) {
            log.warn("Falha ao deletar export: storageKey={}", storageKey, e);
        }
    }

    private Path tenantPath(UUID financialEntityId, String storageKey) {
        final String safeKey;
        try {
            safeKey = UUID.fromString(storageKey).toString();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Chave inválida para armazenamento de exportação");
        }
        Path tenantDir = exportDir.resolve(financialEntityId.toString()).normalize();
        Path filePath = tenantDir.resolve(safeKey).normalize();
        if (!tenantDir.startsWith(exportDir.normalize()) || !filePath.startsWith(tenantDir)) {
            throw new IllegalArgumentException("Caminho inválido para armazenamento de exportação");
        }
        return filePath;
    }

    /**
     * Calcula SHA-256 do conteúdo para verificação de integridade
     */
    private String calculateSha256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Falha ao calcular SHA-256", e);
        }
    }
}
