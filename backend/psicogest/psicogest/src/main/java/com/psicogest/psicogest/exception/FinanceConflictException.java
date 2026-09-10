package com.psicogest.psicogest.exception;

/**
 * Exceção para conflitos no domínio financeiro
 * Ex: duplicação de cobrança, inconsistências
 */
public class FinanceConflictException extends RuntimeException {

    public FinanceConflictException(String message) {
        super(message);
    }

    public FinanceConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
