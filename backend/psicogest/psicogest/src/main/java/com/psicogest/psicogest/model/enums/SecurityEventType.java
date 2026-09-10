package com.psicogest.psicogest.model.enums;

/**
 * Tipos de evento de segurança
 * 
 * Eventos de exportação clínica:
 * - CLINICAL_EXPORT_REQUESTED: solicitação criada
 * - CLINICAL_EXPORT_DOWNLOADED: arquivo baixado
 * - CLINICAL_EXPORT_INTEGRITY_FAILURE: SHA-256 não bate
 * - CLINICAL_EXPORT_EXPIRED: arquivo expirou (90 dias)
 * - MASS_EXPORT_DETECTED: padrão de exfiltração
 * 
 * 28. Eventos de webhook:
 * - WEBHOOK_SIGNATURE_INVALID: assinatura inválida
 * - WEBHOOK_REPLAY_REJECTED: timestamp fora da janela
 * - WEBHOOK_DUPLICATE_RECEIVED: evento duplicado (mesmo eventId + payloadHash)
 * - WEBHOOK_EVENT_COLLISION: mesmo eventId mas payloadHash diferente (suspeito)
 * - WEBHOOK_PROCESSING_FAILED: falha ao processar evento
 */
public enum SecurityEventType {
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    RATE_LIMIT_TRIGGERED,
    MFA_FAILURE,
    ACCESS_DENIED,
    REFRESH_TOKEN_REUSE,
    ACCOUNT_LOCKED,
    PASSWORD_CHANGED,
    MFA_ENROLLED,
    SESSION_REVOKED,
    SECURITY_ALERT,
    AUDIT_LOG_ACCESSED,
    AUDIT_INTEGRITY_FAILURE,
    AUDIT_VERIFICATION_COMPLETED,
    CLINICAL_EXPORT_REQUESTED,
    CLINICAL_EXPORT_DOWNLOADED,
    CLINICAL_EXPORT_INTEGRITY_FAILURE,
    CLINICAL_EXPORT_EXPIRED,
    MASS_EXPORT_DETECTED,
    
    // Webhook events
    WEBHOOK_SIGNATURE_INVALID,
    WEBHOOK_REPLAY_REJECTED,
    WEBHOOK_DUPLICATE_RECEIVED,
    WEBHOOK_EVENT_COLLISION,
    WEBHOOK_PROCESSING_FAILED
}
