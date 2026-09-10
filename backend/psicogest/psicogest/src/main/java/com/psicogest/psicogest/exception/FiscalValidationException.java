package com.psicogest.psicogest.exception;

/**
 * Exceção para validação fiscal
 * 
 * Lançada quando XML ou dados não validam contra XSD
 */
public class FiscalValidationException extends RuntimeException {

    public FiscalValidationException(String message) {
        super(message);
    }

    public FiscalValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
