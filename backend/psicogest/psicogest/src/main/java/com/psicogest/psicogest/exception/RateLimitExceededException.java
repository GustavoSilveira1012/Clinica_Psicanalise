package com.psicogest.psicogest.exception;

public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException() {
        super("Muitas tentativas. Tente novamente mais tarde.");
    }

    public RateLimitExceededException(String message) {
        super(message);
    }
}
