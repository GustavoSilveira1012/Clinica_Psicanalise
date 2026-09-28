package com.psicogest.psicogest.infrastructure.storage;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.psicogest.psicogest.security.tenant.TenantContext;
import com.psicogest.psicogest.security.tenant.TenantContextHolder;
import com.psicogest.psicogest.security.tenant.ConfiguredOrganizationAllowlist;

import lombok.extern.slf4j.Slf4j;

/**
 * Runs retention only for explicitly configured pilot tenants. It deliberately
 * does not enumerate organizations or impersonate an end user.
 */
@Slf4j
@Component
@Profile("production")
@ConditionalOnProperty(name = "app.export-storage.retention.enabled", havingValue = "true")
public class ClinicalExportRetentionScheduler {

    private final ClinicalExportRetentionWorker retentionWorker;
    private final List<UUID> organizationIds;

    public ClinicalExportRetentionScheduler(
            ClinicalExportRetentionWorker retentionWorker,
            @Value("${app.export-storage.retention.organization-ids:}") String configuredOrganizationIds
    ) {
        this.retentionWorker = retentionWorker;
        this.organizationIds = ConfiguredOrganizationAllowlist.parse(configuredOrganizationIds);
        if (organizationIds.isEmpty()) {
            throw new IllegalArgumentException("Allowlist de retenção obrigatória quando o scheduler está habilitado");
        }
    }

    @Scheduled(cron = "${app.export-storage.retention.cron:0 0 3 * * *}", zone = "UTC")
    public RetentionRunResult runConfiguredOrganizations() {
        int processed = 0;
        int selected = 0;
        int finalized = 0;
        int retryPending = 0;
        int failedOrganizations = 0;

        for (UUID organizationId : organizationIds) {
            try {
                // Background work gets a tenant scope, never a fabricated user identity.
                TenantContextHolder.set(new TenantContext(organizationId, null, null));
                ClinicalExportRetentionWorker.RetentionBatchResult result =
                        retentionWorker.processCurrentTenantBatch();
                processed++;
                selected += result.selected();
                finalized += result.finalized();
                retryPending += result.retryPending();
            } catch (RuntimeException exception) {
                // Provider/database exceptions can contain object keys or credentials.
                // Keep logs aggregate-only and continue with the remaining pilot tenants.
                failedOrganizations++;
            } finally {
                TenantContextHolder.clear();
            }
        }

        if (retryPending > 0 || failedOrganizations > 0) {
            log.warn("Clinical export retention needs retry: pending={}, failedTenants={}",
                    retryPending, failedOrganizations);
        }
        return new RetentionRunResult(
                processed, selected, finalized, retryPending, failedOrganizations);
    }

    public record RetentionRunResult(
            int processedOrganizations,
            int selected,
            int finalized,
            int retryPending,
            int failedOrganizations
    ) { }
}
