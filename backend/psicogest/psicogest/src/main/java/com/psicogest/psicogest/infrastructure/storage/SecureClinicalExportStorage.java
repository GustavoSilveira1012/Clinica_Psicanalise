package com.psicogest.psicogest.infrastructure.storage;

import java.io.InputStream;
import java.util.UUID;

/**
 * 19. Abstração de storage para exports clínicos
 * 
 * Implementações futuras:
 * - S3 (AWS)
 * - Azure Blob Storage
 * - Google Cloud Storage
 * - MinIO para DEV
 * 
 * Características:
 * - Criptografia em repouso (delegada ao storage provider)
 * - Sem acesso público (nunca gera URLs abertas)
 * - Integração com auditoria de download
 * - Suporte a verificação de integridade (SHA-256)
 */
public interface SecureClinicalExportStorage {

    /**
     * Armazena export clínico
     * 
     * @param exportId ID único do export (usado na URL segura)
     * @param content bytes do arquivo (PDF, JSON, etc)
     * @param contentType MIME type (application/pdf, application/json, etc)
     * @return metadados do arquivo armazenado
     */
    StoredExport store(
            UUID exportId,
            byte[] content,
            String contentType
    );

    /**
     * Abre stream para download
     * 
     * Chamado apenas após:
     * - Autenticação validada
     * - Autorização verificada
     * - AuditLog registrado
     * 
     * @param storageKey chave retornada por store()
     * @return InputStream para streaming do conteúdo
     */
    InputStream open(
            String storageKey
    );

    /**
     * Deleta export do storage
     * 
     * @param storageKey chave retornada por store()
     */
    void delete(
            String storageKey
    );
}
