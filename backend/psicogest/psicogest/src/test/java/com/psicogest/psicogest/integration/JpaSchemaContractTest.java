package com.psicogest.psicogest.integration;

import jakarta.persistence.Entity;
import org.flywaydb.core.Flyway;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.assertj.core.api.SoftAssertions;
import java.sql.*;
import java.util.*;

/** Aggregates missing mappings rather than hiding all but the first startup failure. */
@Testcontainers
class JpaSchemaContractTest {
    @Container static final PostgreSQLContainer<?> DB = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test void everyMappedColumnExistsInMigratedDatabase() throws Exception {
        Flyway.configure().dataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword())
                .locations("classpath:bd/migration").load().migrate();
        var registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.connection.url", DB.getJdbcUrl())
                .applySetting("hibernate.connection.username", DB.getUsername())
                .applySetting("hibernate.connection.password", DB.getPassword())
                .build();
        try (var db = DriverManager.getConnection(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword())) {
            var sources = new MetadataSources(registry);
            var scanner = new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
            for (var entity : scanner.findCandidateComponents("com.psicogest.psicogest")) {
                sources.addAnnotatedClass(Class.forName(entity.getBeanClassName()));
            }
            var metadata = sources.getMetadataBuilder()
                    .applyPhysicalNamingStrategy(new PhysicalNamingStrategySnakeCaseImpl()).build();
            var dialect = registry.getService(org.hibernate.engine.jdbc.spi.JdbcServices.class).getDialect();
            Set<String> enumTypes = new HashSet<>();
            try (var statement = db.createStatement(); var result = statement.executeQuery("SELECT typname FROM pg_type WHERE typtype='e'")) {
                while (result.next()) enumTypes.add(result.getString(1));
            }
            Map<String, Map<String, SqlColumn>> columns = new HashMap<>();
            try (var result = db.getMetaData().getColumns(null, "public", "%", "%")) {
                while (result.next()) columns.computeIfAbsent(result.getString("TABLE_NAME"), ignored -> new HashMap<>())
                        .put(result.getString("COLUMN_NAME"), new SqlColumn(result.getString("TYPE_NAME"), result.getInt("DATA_TYPE")));
            }
            var softly = new SoftAssertions();
            for (var namespace : metadata.getDatabase().getNamespaces()) {
                for (var table : namespace.getTables()) {
                    if (!table.isPhysicalTable()) continue;
                    softly.assertThat(columns).as("Mapped table %s", table.getName()).containsKey(table.getName());
                    for (var column : table.getColumns()) {
                        var actualColumns = columns.getOrDefault(table.getName(), Map.of());
                        softly.assertThat(actualColumns)
                                .as("Mapped column %s.%s", table.getName(), column.getName()).containsKey(column.getName());
                        if (actualColumns.containsKey(column.getName())) {
                            var actual = actualColumns.get(column.getName());
                            var expected = column.getSqlType(metadata);
                            boolean sameName = normalizeType(actual.name()).equals(normalizeType(expected));
                            // PostgreSQL enum names must match, even when JDBC reports VARCHAR.
                            // Other types use Hibernate's actual dialect compatibility rules.
                            boolean compatible = sameName || (!enumTypes.contains(actual.name())
                                    && dialect.equivalentTypes(column.getSqlTypeCode(metadata), actual.code()));
                            softly.assertThat(compatible)
                                    .as("SQL type %s.%s: expected %s, actual %s", table.getName(), column.getName(), expected, actual.name())
                                    .isTrue();
                        }
                    }
                }
            }
            softly.assertAll();
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    private record SqlColumn(String name, int code) {}

    private String normalizeType(String type) {
        String normalized = type.toLowerCase(Locale.ROOT).replaceAll("\\([^)]*\\)", "").trim();
        return switch (normalized) {
            case "int8", "bigserial" -> "bigint";
            case "int4" -> "integer";
            case "int2" -> "smallint";
            case "bool" -> "boolean";
            case "timestamptz" -> "timestamp with time zone";
            case "bpchar", "character" -> "char";
            case "text", "character varying" -> "varchar";
            case "decimal" -> "numeric";
            case "float8" -> "double precision";
            case "float4" -> "real";
            default -> normalized;
        };
    }
}
