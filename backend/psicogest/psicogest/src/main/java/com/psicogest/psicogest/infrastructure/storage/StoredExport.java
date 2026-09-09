package com.psicogest.psicogest.infrastructure.storage;

/**
 * 19. Metadados do export armazenado
 * 
 * storageKey: identificador para recuperação
 * sha256: hash do conteúdo (integridade)
 * size: tamanho em bytes
 */
public record StoredExport(

        /**
         * Chave para recuperação posterior
         * Formato: depende da implementação
         * Ex: S3: bucket/key, Azure: container/blob, etc
         */
        String storageKey,

        /**
         * SHA-256 do conteúdo (para verificação de integridade)
         */
        String sha256,

        /**
         * Tamanho em bytes
         */
        long size

) {
}
