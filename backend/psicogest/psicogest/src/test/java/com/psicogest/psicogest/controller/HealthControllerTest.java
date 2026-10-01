package com.psicogest.psicogest.controller;

import com.psicogest.psicogest.infrastructure.health.ProviderReadinessService;
import com.psicogest.psicogest.infrastructure.health.MigrationReadiness;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthControllerTest {

    @Test
    void packagedMigrationVersionMatchesCurrentRelease() {
        assertThat(MigrationReadiness.latestPackagedVersion()).isEqualTo("93");
    }

    @Test
    void readinessRejectsAnIncompleteMigrationHistory() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        RedisConnectionFactory redisFactory = mock(RedisConnectionFactory.class);
        ProviderReadinessService providers = mock(ProviderReadinessService.class);
        when(providers.status()).thenReturn(Map.of());
        when(jdbc.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);
        when(jdbc.queryForObject(anyString(), eq(Boolean.class))).thenReturn(true);
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), eq(MigrationReadiness.latestPackagedVersion()))).thenReturn(false);

        var response = new HealthController(jdbc, redisFactory, providers).ready();

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody()).containsEntry("migrations", "DOWN");
    }

    @Test
    void readinessAcceptsTheCompleteMigrationHistory() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        RedisConnectionFactory redisFactory = mock(RedisConnectionFactory.class);
        RedisConnection redis = mock(RedisConnection.class);
        ProviderReadinessService providers = mock(ProviderReadinessService.class);
        when(providers.status()).thenReturn(Map.of());
        when(jdbc.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);
        when(jdbc.queryForObject(anyString(), eq(Boolean.class))).thenReturn(true);
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), eq(MigrationReadiness.latestPackagedVersion()))).thenReturn(true);
        when(redisFactory.getConnection()).thenReturn(redis);
        when(redis.ping()).thenReturn("PONG");
        when(providers.isReady()).thenReturn(true);

        var response = new HealthController(jdbc, redisFactory, providers).ready();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("migrations", "UP");
    }
}
