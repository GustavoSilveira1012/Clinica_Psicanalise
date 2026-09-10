package com.psicogest.psicogest.infrastructure.payment.provider.webhook;

/**
 * Status do webhook no inbox
 * 
 * 17. Status do Inbox
 */
public enum WebhookInboxStatus {
    /**
     * Recebido e verificado
     */
    RECEIVED,

    /**
     * Processamento iniciado
     */
    PROCESSING,

    /**
     * Processado com sucesso
     */
    PROCESSED,

    /**
     * Falhou ao processar
     */
    FAILED,

    /**
     * Rejeitado após múltiplas tentativas
     */
    DEAD_LETTER
}
