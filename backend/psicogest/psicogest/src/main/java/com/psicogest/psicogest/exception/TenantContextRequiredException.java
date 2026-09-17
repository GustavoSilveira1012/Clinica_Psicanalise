package com.psicogest.psicogest.exception;

public class TenantContextRequiredException extends RuntimeException {
    public TenantContextRequiredException(String message) {
        super(message);
    }
}
