package com.psicogest.psicogest.security.incident.sink;

import com.psicogest.psicogest.security.incident.SecurityIncidentNotification;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

/** Sends a minimized, provider-neutral incident payload to an operator-owned webhook. */
@Component
public class SecurityAlertWebhookClient {

    private final RestClient restClient = RestClient.create();

    public void send(String endpoint, SecurityIncidentNotification notification) {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException("O endpoint do alerta de segurança não foi configurado");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("incidentId", notification.incidentId());
        payload.put("sourceAlertId", notification.sourceAlertId());
        payload.put("category", notification.category().name());
        payload.put("severity", notification.severity().name());
        payload.put("status", notification.status().name());
        payload.put("suspectedDataBreach", notification.involvesBreach());
        payload.put("suspectedClinicalDataExposure", notification.involvesClinicalData());
        payload.put("detectedAt", notification.detectedAt());

        restClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }
}
