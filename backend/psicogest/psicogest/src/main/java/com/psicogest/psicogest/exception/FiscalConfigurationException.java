package com.psicogest.psicogest.exception;

/**
 * Exceção para configuração fiscal inválida
 */
public class FiscalConfigurationException extends RuntimeException {

    public FiscalConfigurationException(String message) {
        super(message);
    }

    public FiscalConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
