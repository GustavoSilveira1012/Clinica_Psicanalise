package com.psicogest.psicogest.exception;

/**
 * Exceção para validações no domínio financeiro
 * Ex: desconto maior que valor bruto, valores inválidos
 */
public class FinanceValidationException extends RuntimeException {

    public FinanceValidationException(String message) {
        super(message);
    }

    public FinanceValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
