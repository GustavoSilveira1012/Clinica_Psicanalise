package com.psicogest.psicogest.security.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.Map;

@ConfigurationProperties( prefix = "app.security.crypto" )
public record CryptoProperties(
        String provider,
        String currentKeyId,
        Map<String, String> localKeys
) {
}
