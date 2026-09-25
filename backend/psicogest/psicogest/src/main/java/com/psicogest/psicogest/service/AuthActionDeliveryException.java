package com.psicogest.psicogest.service;

public class AuthActionDeliveryException extends RuntimeException {

    public AuthActionDeliveryException() {
        super("Não foi possível entregar a mensagem de ação de conta");
    }
}
