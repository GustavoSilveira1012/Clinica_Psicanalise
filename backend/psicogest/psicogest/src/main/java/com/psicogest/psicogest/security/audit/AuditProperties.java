package com.psicogest.psicogest.security.audit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(
        prefix = "app.security.audit"
)
public record AuditProperties(

        String currentKeyId,

        Map<String, String> keys

) {
}