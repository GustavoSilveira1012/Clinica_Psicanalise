package com.psicogest.psicogest.security.incident.sink;

import com.psicogest.psicogest.security.incident.SecurityIncidentNotification;
import org.springframework.stereotype.Service;

/**
 * Interface para integração com SIEM.
 * Implementação será adicionada conforme o SIEM for escolhido.
 */
@Service
public class SIEMForwardingService {

    public void forwardIncident(
            SecurityIncidentNotification notification
    ) {

        // Implementação futura:
        // - Formatar evento para SIEM
        // - Enviar via HTTP/syslog/API
        // - Registrar envio
        // TODO: Implementar integração com SIEM (Splunk, Datadog, Elastic, etc)
    }
}
