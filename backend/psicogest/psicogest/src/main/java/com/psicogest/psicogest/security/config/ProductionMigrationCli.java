package com.psicogest.psicogest.security.config;

import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
        boolean supabaseSeedOnly = hasOnlySupabaseSeedFunction(url, user, password);
        return Flyway.configure()
                .dataSource(url, user, password)
                .locations("classpath:bd/migration")
                // Supabase creates public.rls_auto_enable() in an otherwise empty
                // project. Baseline at zero so V01 and every later migration run.
                .baselineOnMigrate(supabaseSeedOnly)
                .baselineVersion("0")
                .validateOnMigrate(true)
                .cleanDisabled(true)
                .load();
    }

    private static boolean hasOnlySupabaseSeedFunction(String url, String user, String password) {
        String sql = """
                SELECT to_regclass('public.flyway_schema_history') IS NULL
                       AND to_regnamespace('app') IS NULL
                       AND NOT EXISTS (
                           SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
                           WHERE n.nspname = 'public')
                       AND NOT EXISTS (
                           SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace
                           WHERE n.nspname = 'public')
                       AND (SELECT count(*) = 1 AND bool_and(
                                   p.proname = 'rls_auto_enable'
                                   AND pg_get_function_identity_arguments(p.oid) = ''
                                   AND pg_get_function_result(p.oid) = 'event_trigger'
                                   AND l.lanname = 'plpgsql')
                            FROM pg_proc p
                            JOIN pg_namespace n ON n.oid = p.pronamespace
                            JOIN pg_language l ON l.oid = p.prolang
                            WHERE n.nspname = 'public')
                """;
        try (Connection connection = DriverManager.getConnection(url, user, password);
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getBoolean(1);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not inspect migration target before Flyway", exception);
        }
    }

    private static String required(Map<String, String> environment, String name) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Migration environment incomplete: " + name);
        }
        return value;
    }
}
