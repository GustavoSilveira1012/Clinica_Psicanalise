package com.psicogest.psicogest.infrastructure.notification.provider;

import com.psicogest.psicogest.model.enums.NotificationChannel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class NotificationProviderRegistry {

    private final Map<NotificationChannel, NotificationProvider> providers;

    public NotificationProviderRegistry(
            List<NotificationProvider> implementations
    ) {
        providers = implementations.stream()
                .collect(Collectors.toUnmodifiableMap(
                        NotificationProvider::channel,
                        Function.identity()
                ));
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
