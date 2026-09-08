package com.psicogest.psicogest.security.incident.sink;

import com.psicogest.psicogest.security.incident.SecurityIncidentNotification;
import org.springframework.stereotype.Service;

/**
 * Interface para integração com cloud monitoring.
 * Implementação será adicionada conforme o provedor for escolhido.
 */
@Service
public class CloudMonitoringService {

    public void publishMetric(
            SecurityIncidentNotification notification
    ) {

        // Implementação futura:
        // - Formatar métrica customizada
        // - Enviar para CloudWatch/Cloud Monitoring/Azure Monitor
        // - Incluir contexto de correlação
        // TODO: Implementar integração com cloud monitoring
    }
}
