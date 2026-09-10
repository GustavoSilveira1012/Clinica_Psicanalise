package com.psicogest.psicogest.exception;

/**
 * Exceção para conflito de idempotência
 * 
 * Quando a mesma chave de idempotência é usada
 * com dados (fingerprint) diferentes
 */
public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String message) {
        super(message);
    }

    public IdempotencyConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
