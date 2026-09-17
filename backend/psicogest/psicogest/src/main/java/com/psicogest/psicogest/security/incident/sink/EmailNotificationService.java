package com.psicogest.psicogest.security.incident.sink;

import com.psicogest.psicogest.security.incident.SecurityIncidentNotification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Interface para envio de emails de alerta de segurança.
 * Implementação será adicionada conforme a infraestrutura de email for definida.
 */
@Service
public class EmailNotificationService {

    private final SecurityAlertWebhookClient webhookClient;
    private final String endpoint;

    public EmailNotificationService(
            SecurityAlertWebhookClient webhookClient,
            @Value("${app.security.alert.email.endpoint:}") String endpoint
    ) {
        this.webhookClient = webhookClient;
        this.endpoint = endpoint;
    }

    public void sendSecurityAlert(
            SecurityIncidentNotification notification
    ) {

        webhookClient.send(endpoint, notification);
    }
}
