package com.psicogest.psicogest.security.pilot;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/** Server-side deny-by-default boundary for modules intentionally excluded from the clinical pilot. */
@Component
public class ClinicalPilotScopeGuard implements HandlerInterceptor {

    private static final List<String> DISABLED_PATHS = List.of(
            "/webhooks/payments/**",
            "/service-invoices/**",
            "/api/v1/notifications/**",
            "/organizations/*/billing",
            "/organizations/*/entitlements",
            "/api/v1/package-plans/**",
            "/api/v1/subscription-plans/**",
            "/api/v1/subscriptions/**",
            "/subscriptions/**",
            "/subscription-plans/**",
            "/patients/*/subscriptions/**",
            "/patients/*/packages/**",
            "/bank-accounts/**",
            "/bank-transactions/**"
    );
    private static final List<String> CLINICAL_DATA_PATHS = List.of(
            "/patients/**",
            "/psychoanalysts/*/appointments/**",
            "/psychoanalysts/*/appointment-series/**",
            "/psychoanalysts/*/availability/**",
            "/psychoanalysts/*/availability-exceptions/**",
            "/medical-records/**",
            "/api/v1/medical-records/**",
            "/api/v1/medical-record-revisions/**",
            "/api/v1/clinical-records/**",
            "/api/v1/receivables/**",
            "/api/v1/payments/**",
            "/api/v1/compliance/**",
            "/api/v1/notifications/**"
    );

    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private final boolean clinicalOnly;
    private final boolean clinicalDataEnabled;
    private final AntPathMatcher paths = new AntPathMatcher();

    public ClinicalPilotScopeGuard(
            @Value("${app.pilot.clinical-only:false}") boolean clinicalOnly,
            @Value("${app.pilot.clinical-data-enabled:true}") boolean clinicalDataEnabled
    ) {
        this.clinicalOnly = clinicalOnly;
        this.clinicalDataEnabled = clinicalDataEnabled;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String contextPath = request.getContextPath();
        String requestPath = request.getRequestURI().substring(contextPath.length());
        boolean clinicalDataBlocked = !clinicalDataEnabled && isClinicalDataPath(requestPath);
        boolean pilotFeatureBlocked = clinicalOnly && isPilotFeatureUnavailable(request.getMethod(), requestPath);
        if (!clinicalDataBlocked && !pilotFeatureBlocked) {
            return true;
        }

        response.setStatus(clinicalDataBlocked ? HttpStatus.LOCKED.value() : HttpStatus.NOT_FOUND.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(clinicalDataBlocked
                ? "{\"code\":\"CLINICAL_DATA_DISABLED\",\"message\":\"Acesso a dados clínicos está desabilitado neste ambiente.\"}"
                : "{\"code\":\"FEATURE_UNAVAILABLE\",\"message\":\"Módulo indisponível neste ambiente.\"}");
        return false;
    }

    boolean isUnavailable(String method, String requestPath) {
        return isClinicalDataPath(requestPath) && !clinicalDataEnabled
                || clinicalOnly && isPilotFeatureUnavailable(method, requestPath);
    }

    private boolean isClinicalDataPath(String requestPath) {
        return CLINICAL_DATA_PATHS.stream().anyMatch(pattern -> paths.match(pattern, requestPath));
    }

    private boolean isPilotFeatureUnavailable(String method, String requestPath) {
        if (DISABLED_PATHS.stream().anyMatch(pattern -> paths.match(pattern, requestPath))) {
            return true;
        }
        return MUTATING_METHODS.contains(method)
                && (paths.match("/api/v1/payments", requestPath)
                || paths.match("/api/v1/payments/**", requestPath));
    }
}
