package com.psicogest.psicogest.security.audit;

/**
 * Eventos anormais de segurança que requerem investigação
 * 
 * Eventos fiscais anormais:
 * - Falha de acesso a credenciais
 * - Falha de autenticação no provedor
 * - Falha de integridade de documentos
 * - Detecção de documentos duplicados
 * - Colisão de operações fiscais
 * - Download suspeito em massa
 */
public enum SecurityEvent {

    FISCAL_CREDENTIAL_ACCESS_FAILURE,

    FISCAL_PROVIDER_AUTH_FAILURE,

    FISCAL_DOCUMENT_INTEGRITY_FAILURE,

    FISCAL_DUPLICATE_DOCUMENT_DETECTED,

    FISCAL_OPERATION_COLLISION,

    FISCAL_SUSPICIOUS_BULK_DOWNLOAD
}
