package com.psicogest.psicogest.domain.privacy;

import java.util.UUID;

/**
 * Identidade já resolvida para a coleta autorizada de dados do titular.
 * Não contém credenciais nem conteúdo clínico.
 */
public record DataSubjectContext(
        UUID requestId,
        Long patientId,
        Long userId
) {
}
