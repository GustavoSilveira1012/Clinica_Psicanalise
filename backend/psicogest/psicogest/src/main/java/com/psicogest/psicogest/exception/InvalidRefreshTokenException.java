package com.psicogest.psicogest.exception;

public class InvalidRefreshTokenException
        extends RuntimeException {

    public InvalidRefreshTokenException() {

        super(
                "Sessão inválida ou expirada"
        );
    }
}