package com.psicogest.psicogest.infrastructure.storage;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.psicogest.psicogest.security.tenant.TenantDatabaseContext;

/** All reads and writes run with the requested tenant's RLS context. */
@Repository
public class JdbcClinicalExportRetentionStore implements ClinicalExportRetentionStore {

    private final JdbcTemplate jdbcTemplate;
    private final TenantDatabaseContext tenantDatabaseContext;
    private final TransactionTemplate tenantTransaction;

    public JdbcClinicalExportRetentionStore(
            JdbcTemplate jdbcTemplate,
            TenantDatabaseContext tenantDatabaseContext,
            PlatformTransactionManager transactionManager
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.tenantDatabaseContext = tenantDatabaseContext;
        this.tenantTransaction = new TransactionTemplate(transactionManager);
        this.tenantTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public List<ExpiredClinicalExport> claimExpired(UUID organizationId, int batchSize) {
        requireOrganization(organizationId);
        if (batchSize < 1 || batchSize > 100) {
            throw new IllegalArgumentException("Lote de retenção deve conter de 1 a 100 exports");
        }
        return tenantTransaction.execute(status -> {
            tenantDatabaseContext.applyOrganization(organizationId);
            return jdbcTemplate.query("""
                    WITH due AS (
                        SELECT id
                          FROM clinical_exports
                         WHERE organization_id = app.current_organization_id()
                           AND storage_key IS NOT NULL
                           AND (status = 'EXPIRING' OR (status = 'READY' AND expires_at <= now()))
                         ORDER BY expires_at NULLS FIRST, id
                         LIMIT ?
                         FOR UPDATE SKIP LOCKED
                    )
                    UPDATE clinical_exports e
                       SET status = 'EXPIRING', version = e.version + 1
                      FROM due
                     WHERE e.id = due.id
                       AND e.organization_id = app.current_organization_id()
                    RETURNING e.id, e.storage_key
                    """, (rs, rowNum) -> new ExpiredClinicalExport(
                    rs.getObject("id", UUID.class), rs.getString("storage_key")), batchSize);
        });
    }

    @Override
    public void markExpired(UUID organizationId, ExpiredClinicalExport export) {
        requireOrganization(organizationId);
        if (export == null || export.id() == null || export.storageKey() == null) {
            throw new IllegalArgumentException("Referência do export expirado inválida");
        }
        tenantTransaction.executeWithoutResult(status -> {
            tenantDatabaseContext.applyOrganization(organizationId);
            int updated = jdbcTemplate.update("""
                    UPDATE clinical_exports
                       SET status = 'EXPIRED', storage_key = NULL,
                           file_sha256 = NULL, file_size = NULL, version = version + 1
                     WHERE id = ?
                       AND organization_id = app.current_organization_id()
                       AND status = 'EXPIRING'
                       AND storage_key = ?
                    """, export.id(), export.storageKey());
            if (updated != 1) {
                throw new IllegalStateException("Estado do export expirado foi alterado concorrentemente");
            }
        });
    }

    private void requireOrganization(UUID organizationId) {
        if (organizationId == null) {
            throw new IllegalArgumentException("Organização obrigatória para retenção de exports");
        }
    }
}
