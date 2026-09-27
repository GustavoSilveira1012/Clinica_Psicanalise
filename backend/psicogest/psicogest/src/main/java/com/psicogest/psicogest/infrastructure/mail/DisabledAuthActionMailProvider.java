package com.psicogest.psicogest.infrastructure.mail;

import com.psicogest.psicogest.service.AuthActionMailProvider;

final class DisabledAuthActionMailProvider implements AuthActionMailProvider {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void send(String recipient, String subject, String body) {
        throw new IllegalStateException("Entrega de e-mail de ações de conta não está configurada");
    }
}
