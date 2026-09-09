package com.psicogest.psicogest.exception;

/**
 * Exceção lançada quando acesso a um recurso é negado
 */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }

    public AccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}
