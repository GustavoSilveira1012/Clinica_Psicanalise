package com.psicogest.psicogest.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

import com.psicogest.psicogest.infrastructure.health.ProviderReadinessService;

@RestController
@RequestMapping("/health")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final RedisConnectionFactory redisConnectionFactory;
    private final ProviderReadinessService providerReadinessService;

    public HealthController(
            JdbcTemplate jdbcTemplate,
            RedisConnectionFactory redisConnectionFactory,
            ProviderReadinessService providerReadinessService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisConnectionFactory = redisConnectionFactory;
        this.providerReadinessService = providerReadinessService;
    }

    @GetMapping({"/live", "/liveness"})
    public Map<String, Object> live() {
        return Map.of("status", "UP", "service", "psicogest-api", "timestamp", Instant.now());
    }

    @GetMapping({"/ready", "/readiness"})
    public ResponseEntity<Map<String, Object>> ready() {
        try {
            jdbcTemplate.queryForObject("select 1", Integer.class);
            boolean migrationTableExists = Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                    """
                    SELECT EXISTS (
                        SELECT 1 FROM information_schema.tables
                        WHERE table_schema = 'public'
                          AND table_name = 'flyway_schema_history'
                    )
                    """,
                    Boolean.class));
            if (!migrationTableExists) {
                throw new IllegalStateException("Migration state unavailable");
            }
            try (RedisConnection connection = redisConnectionFactory.getConnection()) {
                if (!"PONG".equalsIgnoreCase(connection.ping())) {
                    throw new IllegalStateException("Redis unavailable");
                }
            }
            Map<String, Object> providers = providerReadinessService.status();
            if (!providerReadinessService.isReady()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                        "status", "DOWN",
                        "database", "UP",
                        "redis", "UP",
                        "migrations", "UP",
                        "externalProviders", providers,
                        "timestamp", Instant.now()));
            }
            return ResponseEntity.ok(Map.of(
                    "status", "UP",
                    "database", "UP",
                    "redis", "UP",
                    "migrations", "UP",
                    "externalProviders", providers,
                    "timestamp", Instant.now()));
        } catch (RuntimeException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "status", "DOWN",
                    "database", "DOWN",
                    "redis", "DOWN",
                    "migrations", "DOWN",
                    "externalProviders", providerReadinessService.status(),
                    "timestamp", Instant.now()));
        }
    }
}
