package com.psicogest.psicogest.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
public abstract class PostgresIntegrationTest {

    @Container
    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            "postgres:16-alpine")
            .withDatabaseName(
                    "psicogest_test")
            .withUsername(
                    "psicogest_test")
            .withPassword(
                    "psicogest_test");

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