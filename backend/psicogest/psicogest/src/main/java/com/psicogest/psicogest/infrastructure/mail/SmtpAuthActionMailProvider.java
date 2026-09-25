package com.psicogest.psicogest.infrastructure.mail;

import com.psicogest.psicogest.service.AuthActionMailProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

final class SmtpAuthActionMailProvider implements AuthActionMailProvider {

    private final JavaMailSender mailSender;
    private final String from;
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final boolean authenticationRequired;
    private final boolean startTlsEnabled;
    private final boolean startTlsRequired;
    private final boolean sslEnabled;

    SmtpAuthActionMailProvider(JavaMailSender mailSender, String from, String host, int port,
            String username, String password, boolean authenticationRequired,
            boolean startTlsEnabled, boolean startTlsRequired, boolean sslEnabled) {
        this.mailSender = mailSender;
        this.from = from;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.authenticationRequired = authenticationRequired;
        this.startTlsEnabled = startTlsEnabled;
        this.startTlsRequired = startTlsRequired;
        this.sslEnabled = sslEnabled;
    }

    @Override
    public boolean isAvailable() {
        if (host == null || host.isBlank() || port < 1 || port > 65_535) {
            return false;
        }
        if (authenticationRequired
                && (username == null || username.isBlank() || password == null || password.isBlank())) {
            return false;
        }
        return sslEnabled || (startTlsEnabled && startTlsRequired);
    }

    @Override
    public void send(String recipient, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
