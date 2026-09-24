package com.psicogest.psicogest.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.context.annotation.Import;

import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
public abstract class PostgresIntegrationTest {

    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            "postgres:16-alpine")
            .withDatabaseName(
                    "psicogest_test")
            .withUsername(
                    "psicogest_test")
            .withPassword(
                    "psicogest_test");

    // One JVM-wide container matches Spring's cached ApplicationContext lifetime.
    // Ryuk removes it when the test JVM exits; no stale JDBC URL between subclasses.
    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureDatabase(
            DynamicPropertyRegistry registry) {

        registry.add(
                "spring.datasource.url",
                POSTGRES::getJdbcUrl);

        registry.add(
                "spring.datasource.username",
                POSTGRES::getUsername);

        registry.add(
                "spring.datasource.password",
                POSTGRES::getPassword);

        registry.add(
                "spring.datasource.driver-class-name",
                POSTGRES::getDriverClassName);

        registry.add(
                "spring.flyway.enabled",
                () -> true);

        registry.add(
                "spring.jpa.hibernate.ddl-auto",
                () -> "validate");
    }
}
