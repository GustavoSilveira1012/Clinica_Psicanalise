package com.psicogest.psicogest.security.incident.sink;

import com.psicogest.psicogest.security.incident.SecurityIncidentNotification;
import org.springframework.stereotype.Service;

/**
 * Interface para integração com PagerDuty.
 * Implementação será adicionada conforme a integração for definida.
 */
@Service
public class PagerDutyService {

    public void createIncident(
            SecurityIncidentNotification notification
    ) {

        // Implementação futura:
        // - Chamar API de PagerDuty
        // - Criar incident/alert
        // - Atribuir para on-call engineer
        // TODO: Implementar integração com PagerDuty API
    }
}
