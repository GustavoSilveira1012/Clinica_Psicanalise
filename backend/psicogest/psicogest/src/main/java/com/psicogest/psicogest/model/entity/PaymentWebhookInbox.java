package com.psicogest.psicogest.model.entity;

import com.psicogest.psicogest.infrastructure.payment.provider.PaymentProviderType;
import com.psicogest.psicogest.infrastructure.payment.provider.webhook.WebhookInboxStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * 17. Inbox de webhooks de pagamento
 * 
 * Armazena webhooks recebidos com criptografia de envelope
 * Implementa padrão Transactional Inbox para processamento assíncrono robusto
 * 
 * Payload do webhook é criptografado com AES-256-GCM
 * Chave dedicada WEBHOOK_DATA_KEK
 * Nunca reusar CLINICAL_DATA_KEK ou MFA key
 */
@Entity
@Table(name = "payment_webhook_inbox")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentWebhookInbox {

    /**
     * ID único
     */
    @Id
    private UUID id;

    /**
     * Provider que originou o webhook
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private PaymentProviderType provider;

    /**
     * ID único do evento no provider
     * Ex: evt_ABC123 (Stripe), ch_123456789 (MercadoPago)
     * 
     * Essencial para idempotência:
     * UNIQUE(provider, provider_event_id)
     */
    @Column(name = "provider_event_id", nullable = false, length = 255)
    private String providerEventId;

    /**
     * Tipo de evento normalizado
     * Ex: PAYMENT_CONFIRMED, REFUND_FAILED, etc
     */
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    /**
     * SHA-256 do payload original
     * Usado para detectar colisões (mesmo eventId, payload diferente)
     */
    @Column(name = "payload_sha256", nullable = false, length = 64)
    private String payloadSha256;

    /**
     * Status do webhook no inbox
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WebhookInboxStatus status;

    /**
     * Quantidade de tentativas de processamento
     */
    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private Integer attemptCount = 0;

    // ==================== Criptografia ====================

    /**
     * Payload criptografado (JSON)
     * Armazenado em BYTEA
     */
    @Column(name = "encrypted_payload", nullable = false)
    private byte[] encryptedPayload;

    /**
     * IV (Initialization Vector) para AES-GCM
     * 12 bytes (96 bits)
     */
    @Column(name = "payload_iv", nullable = false)
    private byte[] payloadIv;

    /**
     * Data Encryption Key envolvida (envelope encryption)
     * Criptografada com WEBHOOK_DATA_KEK
     */
    @Column(name = "encrypted_dek", nullable = false)
    private byte[] encryptedDek;

    /**
     * Versão do algoritmo criptográfico
     * Atual: 1
     */
    @Column(name = "crypto_version", nullable = false)
    private Integer cryptoVersion;

    /**
     * Algoritmo usado
     * Ex: "AES-256-GCM"
     */
    @Column(name = "crypto_algorithm", nullable = false, length = 30)
    private String cryptoAlgorithm;

    /**
     * ID da chave mestra (WEBHOOK_DATA_KEK)
     * Usado para recuperar chave no KMS
     */
    @Column(name = "key_id", nullable = false, length = 255)
    private String keyId;

    // ==================== Timeline ====================

    /**
     * Quando foi recebido
     */
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    /**
     * Quando iniciou o processamento
     */
    @Column(name = "processing_started_at")
    private Instant processingStartedAt;

    /**
     * Quando foi processado com sucesso
     */
    @Column(name = "processed_at")
    private Instant processedAt;

    /**
     * Quando falhou
     */
    @Column(name = "failed_at")
    private Instant failedAt;

    /**
     * Próxima tentativa em (retry backoff)
     */
    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    /**
     * Último código de erro
     */
    @Column(name = "last_error_code", length = 100)
    private String lastErrorCode;

    /**
     * Criado em
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Auditoria
     */
    @Column(name = "updated_at")
    private Instant updatedAt;
}
