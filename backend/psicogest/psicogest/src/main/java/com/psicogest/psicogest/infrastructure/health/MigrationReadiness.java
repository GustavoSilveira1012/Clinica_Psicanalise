package com.psicogest.psicogest.infrastructure.health;

import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.flywaydb.core.api.MigrationVersion;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;

/** Read-only readiness check for the exact Flyway version shipped with this release. */
public final class MigrationReadiness {
    private static final Pattern MIGRATION_FILE = Pattern.compile("V([0-9_.]+)__.+\\.sql");

    private MigrationReadiness() {
    }

    public static boolean isCurrent(JdbcTemplate jdbc, String expectedVersion) {
        Boolean migrationTableExists = jdbc.queryForObject(
                """
                SELECT EXISTS (
                    SELECT 1 FROM information_schema.tables
                    WHERE table_schema = 'public'
                      AND table_name = 'flyway_schema_history'
                )
                """,
                Boolean.class);
        if (!Boolean.TRUE.equals(migrationTableExists)) {
            return false;
        }
        Boolean current = jdbc.queryForObject(
                """
                SELECT COALESCE((
                    SELECT version = ? AND success
                    FROM public.flyway_schema_history
                    WHERE version IS NOT NULL
                    ORDER BY installed_rank DESC LIMIT 1
                ), FALSE)
                AND NOT EXISTS (
                    SELECT 1 FROM public.flyway_schema_history WHERE NOT success
                )
                """,
                Boolean.class,
                expectedVersion);
        return Boolean.TRUE.equals(current);
    }

    public static String latestPackagedVersion() {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath*:bd/migration/V*__*.sql");
            return Arrays.stream(resources)
                    .map(Resource::getFilename)
                    .map(name -> {
                        Matcher matcher = MIGRATION_FILE.matcher(name == null ? "" : name);
                        if (!matcher.matches()) {
                            throw new IllegalStateException("Invalid packaged migration filename: " + name);
                        }
                        return MigrationVersion.fromVersion(matcher.group(1).replace('_', '.'));
                    })
                    .max(MigrationVersion::compareTo)
                    .orElseThrow(() -> new IllegalStateException("No Flyway migration packaged with the API"))
                    .getVersion();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not inspect packaged Flyway migrations", exception);
        }
    }
}
