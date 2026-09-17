package com.psicogest.psicogest.security.incident.sink;

import com.psicogest.psicogest.security.incident.SecurityIncidentNotification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Interface para integração com PagerDuty.
 * Implementação será adicionada conforme a integração for definida.
 */
@Service
public class PagerDutyService {

    private final SecurityAlertWebhookClient webhookClient;
    private final String endpoint;

    public PagerDutyService(
            SecurityAlertWebhookClient webhookClient,
            @Value("${app.security.alert.pagerduty.endpoint:}") String endpoint
    ) {
        this.webhookClient = webhookClient;
        this.endpoint = endpoint;
    }

    public void createIncident(
            SecurityIncidentNotification notification
    ) {

        webhookClient.send(endpoint, notification);
    }
}
