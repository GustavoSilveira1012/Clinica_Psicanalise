package com.psicogest.psicogest.exception;

/**
 * Exceção para conflito fiscal (duplicação, estado inválido, etc)
 */
public class FiscalConflictException extends RuntimeException {

    public FiscalConflictException(String message) {
        super(message);
    }

    public FiscalConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
