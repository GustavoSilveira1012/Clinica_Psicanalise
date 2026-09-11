package com.psicogest.psicogest.domain.notification;

import com.psicogest.psicogest.model.enums.NotificationChannel;

import java.util.UUID;

/**
 * Configuração de provider pertencente a uma entidade financeira.
 *
 * O identificador do provider é mantido como chave lógica para permitir
 * providers próprios ou compartilhados sem acoplar o domínio à integração.
 */
public record NotificationProviderConfiguration(
        UUID financialEntityId,
        NotificationChannel channel,
        String provider,
        boolean enabled
) {
}
