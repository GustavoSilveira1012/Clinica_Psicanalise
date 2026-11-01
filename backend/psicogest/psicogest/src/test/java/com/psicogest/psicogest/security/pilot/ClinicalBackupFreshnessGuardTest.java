package com.psicogest.psicogest.security.pilot;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ClinicalBackupFreshnessGuardTest {
    private static final Instant NOW = Instant.parse("2026-10-01T15:00:00Z");
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final ClinicalPilotScopeGuard scope = new ClinicalPilotScopeGuard(true, true, true);

    private ClinicalBackupFreshnessGuard guard(boolean enabled, boolean approved) {
        return new ClinicalBackupFreshnessGuard(jdbc, scope, Clock.fixed(NOW, ZoneOffset.UTC), enabled, approved, 55);
    }

    @Test
    void productionComponentStartsWithTheConfiguredConstructor() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("production");
            context.registerBean(JdbcTemplate.class, () -> jdbc);
            context.registerBean(ClinicalPilotScopeGuard.class, () -> scope);
            context.register(ClinicalBackupFreshnessGuard.class);
            context.refresh();
            assertThat(context.getBean(ClinicalBackupFreshnessGuard.class)).isNotNull();
        }
    }

    @Test
    void allowsClinicalWriteOnlyWithinTheVerifiedBackupWindow() throws Exception {
        when(jdbc.queryForMap(anyString())).thenReturn(checkpoint(NOW.minusSeconds(55 * 60), NOW));
        var response = new MockHttpServletResponse();

        assertThat(guard(true, true).preHandle(
                new MockHttpServletRequest("POST", "/patients"), response, new Object())).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void blocksStaleFutureAndMissingBackups() throws Exception {
        for (Map<String, Object> checkpoint : new Map[]{
                checkpoint(NOW.minusSeconds(55 * 60 + 1), NOW),
                checkpoint(NOW.plusSeconds(1), NOW),
                new HashMap<>()
        }) {
            when(jdbc.queryForMap(anyString())).thenReturn(checkpoint);
            var response = new MockHttpServletResponse();
            assertThat(guard(true, true).preHandle(
                    new MockHttpServletRequest("PATCH", "/medical-records/3"), response, new Object())).isFalse();
            assertThat(response.getStatus()).isEqualTo(503);
            assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
            assertThat(response.getContentAsString()).contains("CLINICAL_BACKUP_STALE");
        }
    }

    @Test
    void blocksUnconfirmedOrFutureConfirmation() throws Exception {
        for (Map<String, Object> checkpoint : new Map[]{
                checkpoint(NOW.minusSeconds(10), NOW.minusSeconds(11)),
                checkpoint(NOW.minusSeconds(10), NOW.plusSeconds(1))
        }) {
            when(jdbc.queryForMap(anyString())).thenReturn(checkpoint);
            var response = new MockHttpServletResponse();
            assertThat(guard(true, true).preHandle(
                    new MockHttpServletRequest("POST", "/patients"), response, new Object())).isFalse();
            assertThat(response.getStatus()).isEqualTo(503);
        }
    }

    @Test
    void blocksWhenCheckpointQueryFails() throws Exception {
        when(jdbc.queryForMap(anyString()))
                .thenThrow(new IllegalStateException("database unavailable"));
        var response = new MockHttpServletResponse();
        assertThat(guard(true, true).preHandle(
                new MockHttpServletRequest("POST", "/patients"), response, new Object())).isFalse();
        assertThat(response.getStatus()).isEqualTo(503);
    }

    @Test
    void blocksClinicAndPractitionerWritesWhenBackupIsMissing() throws Exception {
        when(jdbc.queryForMap(anyString())).thenThrow(new IllegalStateException("checkpoint unavailable"));
        for (String path : new String[]{"/clinics", "/clinic-memberships/3/periods", "/psychoanalysts"}) {
            var response = new MockHttpServletResponse();
            assertThat(guard(true, true).preHandle(
                    new MockHttpServletRequest("POST", path), response, new Object())).isFalse();
            assertThat(response.getStatus()).isEqualTo(503);
        }
    }

    @Test
    void doesNotQueryBackupForClinicalReadsOrUnrelatedPathsOrClosedRelease() throws Exception {
        var response = new MockHttpServletResponse();
        assertThat(guard(true, true).preHandle(
                new MockHttpServletRequest("GET", "/patients"), response, new Object())).isTrue();
        assertThat(guard(true, true).preHandle(
                new MockHttpServletRequest("POST", "/organizations"), response, new Object())).isTrue();
        assertThat(guard(false, true).preHandle(
                new MockHttpServletRequest("POST", "/patients"), response, new Object())).isTrue();
        assertThat(guard(true, false).preHandle(
                new MockHttpServletRequest("POST", "/patients"), response, new Object())).isTrue();
        verifyNoInteractions(jdbc);
    }

    @Test
    void refusesAWindowLongerThanTheAcceptedMaximum() {
        assertThatThrownBy(() -> new ClinicalBackupFreshnessGuard(
                jdbc, scope, Clock.fixed(NOW, ZoneOffset.UTC), true, true, 60))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Map<String, Object> checkpoint(Instant started, Instant confirmed) {
        return Map.of("snapshot_started_at", Timestamp.from(started),
                "confirmed_at", Timestamp.from(confirmed));
    }
}
