package com.psicogest.psicogest.security.incident.sink;

import com.psicogest.psicogest.security.incident.SecurityIncidentNotification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Interface para integração com SIEM.
 * Implementação será adicionada conforme o SIEM for escolhido.
 */
@Service
public class SIEMForwardingService {

    private final SecurityAlertWebhookClient webhookClient;
    private final String endpoint;

    public SIEMForwardingService(
            SecurityAlertWebhookClient webhookClient,
            @Value("${app.security.alert.siem.endpoint:}") String endpoint
    ) {
        this.webhookClient = webhookClient;
        this.endpoint = endpoint;
    }

    public void forwardIncident(
            SecurityIncidentNotification notification
    ) {

        webhookClient.send(endpoint, notification);
    }
}
