package com.psicogest.psicogest.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URI;

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
                || uri.getFragment() != null
                || !hasVerifiedTls(uri.getRawQuery())) {
            throw invalidUrl();
        }
    }

    private static boolean hasVerifiedTls(String query) {
        // Reject duplicate sslmode and JDBC connection overrides. A later option can
        // silently undo hostname verification or redirect the connection target.
        return "sslmode=verify-full".equals(query);
    }

    private static IllegalStateException invalidUrl() {
        return new IllegalStateException(
                "DATABASE_URL deve ser JDBC PostgreSQL sem credenciais embutidas e exigir sslmode=verify-full");
    }
}
