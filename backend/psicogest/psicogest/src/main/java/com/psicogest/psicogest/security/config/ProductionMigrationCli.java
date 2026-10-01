package com.psicogest.psicogest.security.config;

import java.net.URI;
import java.util.Map;
import org.flywaydb.core.Flyway;

/** One-shot schema migration; never started by the HTTP serving process. */
public final class ProductionMigrationCli {
    private static final String CONFIRMATION = "APPLY-PSICOGEST-MIGRATIONS";

    private ProductionMigrationCli() {
    }

    public static void main(String[] args) {
        if (args.length != 0) {
            throw new IllegalArgumentException("Migration CLI does not accept command-line secrets");
        }
        migrateFromEnvironment(System.getenv());
    }

    static void migrateFromEnvironment(Map<String, String> environment) {
        String url = required(environment, "DATABASE_URL");
        String user = required(environment, "MIGRATION_DATABASE_USERNAME");
        String password = required(environment, "MIGRATION_DATABASE_PASSWORD");
        ProductionDatabaseUrlValidator.validate(url);

        URI uri = URI.create(url.substring("jdbc:".length()));
        if (uri.getPort() < 1) {
            throw new IllegalStateException("Migration JDBC URL must include an explicit port");
        }
        String target = uri.getHost() + ":" + uri.getPort() + uri.getPath();
        if (!target.equals(environment.get("MIGRATION_APPROVED_TARGET"))) {
            throw new IllegalStateException("Migration target was not explicitly approved");
        }
        if (!CONFIRMATION.equals(environment.get("MIGRATION_CONFIRMATION"))) {
            throw new IllegalStateException("Migration confirmation did not match");
        }
        flyway(url, user, password).migrate();
    }

    static Flyway flyway(String url, String user, String password) {
        return Flyway.configure()
                .dataSource(url, user, password)
                .locations("classpath:bd/migration")
                .baselineOnMigrate(false)
                .validateOnMigrate(true)
                .cleanDisabled(true)
                .load();
    }

    private static String required(Map<String, String> environment, String name) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Migration environment incomplete: " + name);
        }
        return value;
    }
}
