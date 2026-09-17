package com.psicogest.psicogest.security.incident.sink;

import com.psicogest.psicogest.security.incident.SecurityIncidentNotification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Interface para integração com cloud monitoring.
 * Implementação será adicionada conforme o provedor for escolhido.
 */
@Service
public class CloudMonitoringService {

    private final SecurityAlertWebhookClient webhookClient;
    private final String endpoint;

    public CloudMonitoringService(
            SecurityAlertWebhookClient webhookClient,
            @Value("${app.security.alert.cloud-monitoring.endpoint:}") String endpoint
    ) {
        this.webhookClient = webhookClient;
        this.endpoint = endpoint;
    }

    public void publishMetric(
            SecurityIncidentNotification notification
    ) {

        webhookClient.send(endpoint, notification);
    }
}
