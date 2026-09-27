package com.psicogest.psicogest.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Arrays;

/** Refuses production database URLs that do not verify the server certificate and hostname. */
@Component
@Profile("production")
public class ProductionDatabaseUrlValidator {

    public ProductionDatabaseUrlValidator(@Value("${spring.datasource.url}") String databaseUrl) {
        validate(databaseUrl);
    }

    public static void validate(String databaseUrl) {
        if (databaseUrl == null || databaseUrl.isBlank() || !databaseUrl.startsWith("jdbc:")) {
            throw invalidUrl();
        }

        URI uri;
        try {
            uri = URI.create(databaseUrl.substring("jdbc:".length()));
        } catch (IllegalArgumentException exception) {
            throw invalidUrl();
        }

        if (!"postgresql".equalsIgnoreCase(uri.getScheme())
                || uri.getHost() == null
                || uri.getHost().isBlank()
                || uri.getUserInfo() != null
                || uri.getPath() == null
                || uri.getPath().length() < 2
                || !hasVerifiedTls(uri.getRawQuery())) {
            throw invalidUrl();
        }
    }

    private static boolean hasVerifiedTls(String query) {
        if (query == null || query.isBlank()) {
            return false;
        }
        return Arrays.stream(query.split("&"))
                .map(parameter -> parameter.split("=", 2))
                .anyMatch(pair -> pair.length == 2
                        && "sslmode".equalsIgnoreCase(pair[0])
                        && "verify-full".equalsIgnoreCase(pair[1]));
    }

    private static IllegalStateException invalidUrl() {
        return new IllegalStateException(
                "DATABASE_URL deve ser JDBC PostgreSQL sem credenciais embutidas e exigir sslmode=verify-full");
    }
}
