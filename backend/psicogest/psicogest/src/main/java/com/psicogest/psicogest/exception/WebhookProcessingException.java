package com.psicogest.psicogest.exception;

/**
 * 39. Exceção de processamento de webhook
 * 
 * Classifica erro como retryable ou não:
 * - Retryable: DB timeout, lock timeout, KMS temporariamente indisponível, network
 * - Não retryable: assinatura inválida, schema impossível, provider desconhecido, event collision
 */
public class WebhookProcessingException extends RuntimeException {

    private final boolean retryable;

    /**
     * Cria exceção retryable
     */
    public WebhookProcessingException(String message) {
        super(message);
        this.retryable = true;
    }

    /**
     * Cria exceção com classificação explícita
     */
    public WebhookProcessingException(
            String message,
            boolean retryable
    ) {
        super(message);
        this.retryable = retryable;
    }

    /**
     * Cria exceção com causa e classificação
     */
    public WebhookProcessingException(
            String message,
            Throwable cause,
            boolean retryable
    ) {
        super(message, cause);
        this.retryable = retryable;
    }

    /**
     * Retorna se o erro é retryable
     */
    public boolean isRetryable() {
        return retryable;
    }
}
