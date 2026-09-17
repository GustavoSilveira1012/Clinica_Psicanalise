package com.psicogest.psicogest.infrastructure.notification.provider;

import com.psicogest.psicogest.model.enums.NotificationChannel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.EnumMap;
import java.util.Map;

@Component
public class NotificationProviderRegistry {

    private final Map<NotificationChannel, NotificationProvider> providers;

    public NotificationProviderRegistry(
            List<NotificationProvider> implementations
    ) {
        EnumMap<NotificationChannel, NotificationProvider> indexed =
                new EnumMap<>(NotificationChannel.class);
        for (NotificationProvider implementation : implementations) {
            if (implementation == null || implementation.channel() == null) {
                throw new NotificationProviderException("Provider de notificação inválido");
            }
            NotificationProvider previous = indexed.putIfAbsent(
                    implementation.channel(), implementation);
            if (previous != null) {
                throw new NotificationProviderException(
                        "Mais de um provider configurado para o canal "
                                + implementation.channel());
            }
        }
        providers = Map.copyOf(indexed);
    }

    public NotificationProvider get(NotificationChannel channel) {
        NotificationProvider provider = providers.get(channel);

        if (provider == null) {
            throw new NotificationProviderException(
                    "Canal não configurado"
            );
        }

        return provider;
    }
}
