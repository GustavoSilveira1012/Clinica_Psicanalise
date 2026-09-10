package com.psicogest.psicogest.exception;

/**
 * Exceção para transições inválidas de estado no domínio financeiro
 * Ex: tentar marcar como pago um receivable já pago
 */
public class InvalidFinanceTransitionException extends RuntimeException {

    public InvalidFinanceTransitionException(String message) {
        super(message);
    }

    public InvalidFinanceTransitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
