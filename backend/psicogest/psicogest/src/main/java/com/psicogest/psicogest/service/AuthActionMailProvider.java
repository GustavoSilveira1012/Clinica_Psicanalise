package com.psicogest.psicogest.service;

public interface AuthActionMailProvider {

    boolean isAvailable();

    void send(String recipient, String subject, String body);
}
