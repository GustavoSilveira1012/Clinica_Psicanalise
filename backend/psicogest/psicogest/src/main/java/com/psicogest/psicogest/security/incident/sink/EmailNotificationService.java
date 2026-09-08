package com.psicogest.psicogest.security.incident.sink;

import com.psicogest.psicogest.security.incident.SecurityIncidentNotification;
import org.springframework.stereotype.Service;

/**
 * Interface para envio de emails de alerta de segurança.
 * Implementação será adicionada conforme a infraestrutura de email for definida.
 */
@Service
public class EmailNotificationService {

    public void sendSecurityAlert(
            SecurityIncidentNotification notification
    ) {

        // Implementação futura:
        // - Formatar email HTML
        // - Enviar via SMTP/SES/SendGrid
        // - Registrar tentativa de envio
        // TODO: Implementar integração com provedor de email
    }
}
