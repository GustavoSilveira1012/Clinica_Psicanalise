package com.psicogest.psicogest.exception;

public class SecurityInfrastructureException extends RuntimeException {

    public SecurityInfrastructureException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }

    public SecurityInfrastructureException(String message) {
        super(message);
    }
}
