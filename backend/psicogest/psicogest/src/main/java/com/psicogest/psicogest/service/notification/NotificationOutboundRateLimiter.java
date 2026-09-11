package com.psicogest.psicogest.service.notification;

import com.psicogest.psicogest.model.enums.NotificationChannel;

import java.util.UUID;

/**
 * Impede que uma entidade financeira ultrapasse o limite de envios de saída.
 *
 * A implementação pode usar Redis ou outro mecanismo distribuído; o contrato
 * não expõe detalhes do armazenamento nem do algoritmo de limitação.
 */
public interface NotificationOutboundRateLimiter {

    void check(
            NotificationChannel channel,
            UUID financialEntityId
    );
}
