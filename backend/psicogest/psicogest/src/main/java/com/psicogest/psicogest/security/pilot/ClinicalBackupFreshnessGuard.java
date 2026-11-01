package com.psicogest.psicogest.security.pilot;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

/** Denies clinical writes when an independently verified backup is too old. */
@Component
@Profile("production")
public class ClinicalBackupFreshnessGuard implements HandlerInterceptor, WebMvcConfigurer {

    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final String LAST_BACKUP_SQL = """
            SELECT snapshot_started_at, confirmed_at
              FROM app.clinical_backup_checkpoints
             WHERE id = 1
            """;

    private final JdbcTemplate jdbc;
    private final ClinicalPilotScopeGuard clinicalScope;
    private final Clock clock;
    private final Duration maximumAge;
    private final boolean clinicalDataEnabled;
    private final boolean releaseApproved;

    @Autowired
    public ClinicalBackupFreshnessGuard(
            JdbcTemplate jdbc,
            ClinicalPilotScopeGuard clinicalScope,
            @Value("${app.pilot.clinical-data-enabled:false}") boolean clinicalDataEnabled,
            @Value("${app.pilot.clinical-data-release-approved:false}") boolean releaseApproved,
            @Value("${app.pilot.backup-maximum-age-minutes:55}") long maximumAgeMinutes
    ) {
        this(jdbc, clinicalScope, Clock.systemUTC(), clinicalDataEnabled, releaseApproved, maximumAgeMinutes);
    }

    ClinicalBackupFreshnessGuard(
            JdbcTemplate jdbc,
            ClinicalPilotScopeGuard clinicalScope,
            Clock clock,
            boolean clinicalDataEnabled,
            boolean releaseApproved,
            long maximumAgeMinutes
    ) {
        if (maximumAgeMinutes < 1 || maximumAgeMinutes > 55) {
            throw new IllegalArgumentException("Backup clinical freshness must be between 1 and 55 minutes");
        }
        this.jdbc = jdbc;
        this.clinicalScope = clinicalScope;
        this.clock = clock;
        this.maximumAge = Duration.ofMinutes(maximumAgeMinutes);
        this.clinicalDataEnabled = clinicalDataEnabled;
        this.releaseApproved = releaseApproved;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (!clinicalDataEnabled || !releaseApproved
                || !MUTATING_METHODS.contains(request.getMethod())
                || !clinicalScope.isClinicalDataPath(path)) {
            return true;
        }

        try {
            var checkpoint = jdbc.queryForMap(LAST_BACKUP_SQL);
            Timestamp startedAt = (Timestamp) checkpoint.get("snapshot_started_at");
            Timestamp confirmedAt = (Timestamp) checkpoint.get("confirmed_at");
            if (startedAt != null && confirmedAt != null) {
                Instant now = Instant.now(clock);
                Instant started = startedAt.toInstant();
                Instant confirmed = confirmedAt.toInstant();
                Duration age = Duration.between(started, now);
                if (!age.isNegative() && age.compareTo(maximumAge) <= 0
                        && !confirmed.isBefore(started) && !confirmed.isAfter(now)) {
                    return true;
                }
            }
        } catch (RuntimeException ignored) {
            // Missing schema, privileges, checkpoint or database availability must fail closed.
        }

        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setHeader("Cache-Control", "no-store");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"code\":\"CLINICAL_BACKUP_STALE\",\"message\":\"Gravações clínicas temporariamente indisponíveis.\"}");
        return false;
    }

}
