package com.psicogest.psicogest.domain.privacy;

import com.psicogest.psicogest.model.enums.PrivacyExportStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Referência segura ao arquivo de exportação do titular.
 * O conteúdo fica em storage privado; o domínio mantém apenas metadados de
 * integridade, expiração e auditoria do download.
 */
public record PrivacyExportJob(
        UUID id,
        UUID requestId,
        PrivacyExportStatus status,
        String storageKey,
        String fileSha256,
        Instant expiresAt,
        Instant downloadedAt
) {
}
