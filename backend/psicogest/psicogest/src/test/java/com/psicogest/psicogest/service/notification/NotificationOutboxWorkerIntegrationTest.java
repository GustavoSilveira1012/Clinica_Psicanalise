package com.psicogest.psicogest.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.psicogest.psicogest.security.tenant.TenantContext;
import com.psicogest.psicogest.security.tenant.TenantContextHolder;
import com.psicogest.psicogest.security.tenant.TenantDatabaseContext;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.postgresql.ds.PGSimpleDataSource;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class NotificationOutboxWorkerIntegrationTest {

    private static final AtomicLong USER_IDS = new AtomicLong(990001);

    @Container
    static final PostgreSQLContainer<?> DB = new PostgreSQLContainer<>("postgres:16-alpine");

    private static JdbcTemplate jdbcTemplate;
    private final UUID organizationId = UUID.randomUUID();
    private final UUID successEventId = UUID.randomUUID();
    private final UUID failingEventId = UUID.randomUUID();
    private final long userId = USER_IDS.getAndIncrement();
    private NotificationOutboxWorker worker;
    private final AtomicInteger successCalls = new AtomicInteger();
    private final AtomicInteger failingCalls = new AtomicInteger();

    @BeforeAll
    static void migrateSchema() {
        Flyway.configure()
                .dataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword())
                .locations("classpath:bd/migration")
                .baselineOnMigrate(false)
                .cleanDisabled(true)
                .load()
                .migrate();

        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(DB.getJdbcUrl());
        dataSource.setUser(DB.getUsername());
        dataSource.setPassword(DB.getPassword());
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        jdbcTemplate.update("""
                INSERT INTO users(id, name, email, password_hash, role)
                VALUES (?, 'Synthetic worker owner', ?, 'not-a-usable-hash', 'CLINIC_ADMIN')
                """, userId, "worker-owner-" + userId + "@example.invalid");
        jdbcTemplate.update("""
                INSERT INTO organizations(id, name, slug, type, owner_user_id, created_at, updated_at)
                VALUES (?, 'Synthetic worker organization', ?, 'CLINIC', ?, now(), now())
                """, organizationId, "synthetic-worker-" + organizationId, userId);
        insertOutboxEvent(successEventId, "APPOINTMENT_CREATED", "{\"patientFirstName\":\"Teste\",\"optional\":null}", Instant.now().minusSeconds(10));
        insertOutboxEvent(failingEventId, "APPOINTMENT_RESCHEDULED", "{}", Instant.now().minusSeconds(5));

        NotificationEventHandler handler = new NotificationEventHandler() {
            @Override
            public boolean supports(String eventType) {
                return eventType.startsWith("APPOINTMENT_");
            }

            @Override
            public void handle(com.psicogest.psicogest.domain.event.DomainEvent event) {
                StoredNotificationDomainEvent stored = (StoredNotificationDomainEvent) event;
                if (stored.id().equals(failingEventId)) {
                    failingCalls.incrementAndGet();
                    throw new IllegalStateException("synthetic handler failure with private payload");
                }
                assertThat(stored.payload()).containsEntry("patientFirstName", "Teste").containsKey("optional");
                assertThat(stored.payload().get("optional")).isNull();
                successCalls.incrementAndGet();
            }
        };

        worker = new NotificationOutboxWorker(
                jdbcTemplate,
                new TenantDatabaseContext(jdbcTemplate),
                new ObjectMapper(),
                List.of(handler),
                new DataSourceTransactionManager(jdbcTemplate.getDataSource()),
                2,
                300,
                30);
        TenantContextHolder.set(new TenantContext(organizationId, userId, null));
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void retriesToDeadLetterWithoutChangingSharedOutboxAndIsIdempotent() {
        assertThat(worker.processCurrentTenantBatch(1)).isEqualTo(1);
        assertThat(statusOf(successEventId)).isEqualTo("PROCESSED");
        assertThat(outboxStatusOf(successEventId)).isEqualTo("PENDING");

        assertThat(worker.processCurrentTenantBatch(1)).isZero();
        assertThat(statusOf(failingEventId)).isEqualTo("RETRY");
        assertThat(attemptsOf(failingEventId)).isEqualTo(1);
        assertThat(lastErrorOf(failingEventId)).isEqualTo("IllegalStateException");

        var transaction = new org.springframework.transaction.support.TransactionTemplate(
                new DataSourceTransactionManager(jdbcTemplate.getDataSource()));
        transaction.executeWithoutResult(status -> {
            new TenantDatabaseContext(jdbcTemplate).applyOrganization(organizationId);
            jdbcTemplate.update("UPDATE notification_outbox_receipts SET next_attempt_at = now() - interval '1 second' WHERE event_id = ?", failingEventId);
        });
        assertThat(worker.processCurrentTenantBatch(1)).isZero();
        assertThat(statusOf(failingEventId)).isEqualTo("DEAD_LETTER");
        assertThat(attemptsOf(failingEventId)).isEqualTo(2);
        assertThat(failingCalls.get()).isEqualTo(2);

        assertThat(worker.processCurrentTenantBatch(1)).isZero();
        assertThat(successCalls.get()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM notification_outbox_receipts WHERE organization_id = ?", Integer.class, organizationId)).isEqualTo(2);
    }

    @Test
    void recordsUnsupportedEventsAsIgnoredInsteadOfSilentlyProcessedOrRetried() {
        UUID unsupportedEventId = UUID.randomUUID();
        insertOutboxEvent(unsupportedEventId, "UNRELATED_DOMAIN_EVENT", "{}", Instant.now());

        assertThat(worker.processCurrentTenantBatch(10)).isEqualTo(1);
        assertThat(statusOf(successEventId)).isEqualTo("PROCESSED");
        assertThat(statusOf(failingEventId)).isEqualTo("RETRY");
        assertThat(statusOf(unsupportedEventId)).isEqualTo("IGNORED");
        assertThat(lastErrorOf(unsupportedEventId)).isEqualTo("NO_NOTIFICATION_HANDLER");

        assertThat(worker.processCurrentTenantBatch(10)).isZero();
        assertThat(attemptsOf(unsupportedEventId)).isEqualTo(1);
        assertThat(successCalls.get()).isEqualTo(1);
    }

    @Test
    void requiresTenantContextAndRejectsUnboundedBatches() {
        TenantContextHolder.clear();
        assertThatThrownBy(() -> worker.processCurrentTenantBatch(1))
                .isInstanceOf(com.psicogest.psicogest.exception.AccessDeniedException.class);
        TenantContextHolder.set(new TenantContext(organizationId, userId, null));
        assertThatThrownBy(() -> worker.processCurrentTenantBatch(101))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void concurrentWorkersDoNotDispatchTheSameClaimedEvent() throws Exception {
        UUID concurrentOrganizationId = UUID.randomUUID();
        UUID concurrentEventId = UUID.randomUUID();
        long concurrentUserId = USER_IDS.getAndIncrement();
        jdbcTemplate.update("""
                INSERT INTO users(id, name, email, password_hash, role)
                VALUES (?, 'Synthetic concurrent owner', ?, 'not-a-usable-hash', 'CLINIC_ADMIN')
                """, concurrentUserId, "concurrent-owner-" + concurrentUserId + "@example.invalid");
        jdbcTemplate.update("""
                INSERT INTO organizations(id, name, slug, type, owner_user_id, created_at, updated_at)
                VALUES (?, 'Synthetic concurrent organization', ?, 'CLINIC', ?, now(), now())
                """, concurrentOrganizationId, "synthetic-concurrent-" + concurrentOrganizationId, concurrentUserId);
        insertOutboxEvent(concurrentEventId, "CONCURRENT_TEST", "{}", Instant.now(), concurrentOrganizationId);

        CountDownLatch handlerStarted = new CountDownLatch(1);
        CountDownLatch releaseHandler = new CountDownLatch(1);
        AtomicInteger dispatchCount = new AtomicInteger();
        NotificationEventHandler blockingHandler = new NotificationEventHandler() {
            @Override
            public boolean supports(String eventType) {
                return "CONCURRENT_TEST".equals(eventType);
            }

            @Override
            public void handle(com.psicogest.psicogest.domain.event.DomainEvent event) {
                dispatchCount.incrementAndGet();
                handlerStarted.countDown();
                try {
                    if (!releaseHandler.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("synthetic handler timed out");
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("synthetic handler interrupted", interrupted);
                }
            }
        };
        NotificationOutboxWorker concurrentWorker = new NotificationOutboxWorker(
                jdbcTemplate,
                new TenantDatabaseContext(jdbcTemplate),
                new ObjectMapper(),
                List.of(blockingHandler),
                new DataSourceTransactionManager(jdbcTemplate.getDataSource()),
                2,
                300,
                30);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> {
                TenantContextHolder.set(new TenantContext(concurrentOrganizationId, concurrentUserId, null));
                try {
                    return concurrentWorker.processCurrentTenantBatch(10);
                } finally {
                    TenantContextHolder.clear();
                }
            });
            assertThat(handlerStarted.await(5, TimeUnit.SECONDS)).as("first worker reaches handler").isTrue();

            var second = executor.submit(() -> {
                TenantContextHolder.set(new TenantContext(concurrentOrganizationId, concurrentUserId, null));
                try {
                    return concurrentWorker.processCurrentTenantBatch(10);
                } finally {
                    TenantContextHolder.clear();
                }
            });
            assertThat(second.get(5, TimeUnit.SECONDS)).isZero();
            releaseHandler.countDown();

            assertThat(first.get(5, TimeUnit.SECONDS)).isEqualTo(1);
            assertThat(dispatchCount.get()).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT status FROM notification_outbox_receipts WHERE event_id = ?",
                    String.class,
                    concurrentEventId)).isEqualTo("PROCESSED");
        } finally {
            releaseHandler.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private void insertOutboxEvent(UUID id, String type, String payload, Instant occurredAt) {
        insertOutboxEvent(id, type, payload, occurredAt, organizationId);
    }

    private void insertOutboxEvent(UUID id, String type, String payload, Instant occurredAt, UUID eventOrganizationId) {
        jdbcTemplate.update("""
                INSERT INTO domain_event_outbox(
                    id, aggregate_type, aggregate_id, event_type, deduplication_key,
                    payload, status, occurred_at, created_at, organization_id
                ) VALUES (?, 'SYNTHETIC', ?, ?, ?, ?::jsonb, 'PENDING', ?, now(), ?)
                """, id, id.toString(), type, "synthetic-worker-" + id, payload,
                java.sql.Timestamp.from(occurredAt), eventOrganizationId);
    }

    private String statusOf(UUID eventId) {
        return jdbcTemplate.queryForObject("SELECT status FROM notification_outbox_receipts WHERE event_id = ?", String.class, eventId);
    }

    private Integer attemptsOf(UUID eventId) {
        return jdbcTemplate.queryForObject("SELECT attempt_count FROM notification_outbox_receipts WHERE event_id = ?", Integer.class, eventId);
    }

    private String lastErrorOf(UUID eventId) {
        return jdbcTemplate.queryForObject("SELECT last_error_code FROM notification_outbox_receipts WHERE event_id = ?", String.class, eventId);
    }

    private String outboxStatusOf(UUID eventId) {
        return jdbcTemplate.queryForObject("SELECT status FROM domain_event_outbox WHERE id = ?", String.class, eventId);
    }
}
