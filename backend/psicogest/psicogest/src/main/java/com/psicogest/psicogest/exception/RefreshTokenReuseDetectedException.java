package com.psicogest.psicogest.exception;

public class RefreshTokenReuseDetectedException
        extends RuntimeException {

    public RefreshTokenReuseDetectedException() {

        super(
                "Sessão encerrada por motivo de segurança"
        );
    }
}