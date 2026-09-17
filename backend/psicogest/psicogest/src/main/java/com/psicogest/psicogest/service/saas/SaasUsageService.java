package com.psicogest.psicogest.service.saas;

import com.psicogest.psicogest.exception.SaasUsageException;
import com.psicogest.psicogest.model.entity.saas.SaasUsageEvent;
import com.psicogest.psicogest.repository.SaasUsageEventRepository;
import com.psicogest.psicogest.security.tenant.TenantContextResolver;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

/** Registra usage uma vez e atualiza o agregado mensal de forma atômica. */
@Service
public class SaasUsageService {

    private final TenantContextResolver tenantContextResolver;
    private final SaasUsageEventRepository usageEventRepository;
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public SaasUsageService(
            TenantContextResolver tenantContextResolver,
            SaasUsageEventRepository usageEventRepository,
            JdbcTemplate jdbcTemplate,
            Clock clock
    ) {
        this.tenantContextResolver = tenantContextResolver;
        this.usageEventRepository = usageEventRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Transactional
    public boolean record(
            Authentication authentication,
            UUID organizationId,
            String featureCode,
            long quantity,
            String idempotencyKey
    ) {
        tenantContextResolver.resolve(authentication, organizationId);
        validate(featureCode, quantity, idempotencyKey);

        String normalizedFeature = featureCode.trim().toUpperCase(java.util.Locale.ROOT);
        String normalizedKey = idempotencyKey.trim();
        var now = clock.instant();
        int inserted;
        try {
            inserted = jdbcTemplate.update(
                    """
                    INSERT INTO saas_usage_events
                        (id, organization_id, feature_code, quantity,
                         idempotency_key, occurred_at, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (organization_id, idempotency_key) DO NOTHING
                    """,
                    UUID.randomUUID(),
                    organizationId,
                    normalizedFeature,
                    quantity,
                    normalizedKey,
                    Timestamp.from(now),
                    Timestamp.from(now)
            );
        } catch (DataIntegrityViolationException exception) {
            throw new SaasUsageException(
                    "Não foi possível registrar o uso de forma idempotente");
        }

        if (inserted == 0) {
            return false;
        }

        LocalDate periodStart = now.atZone(ZoneOffset.UTC).toLocalDate().withDayOfMonth(1);
        jdbcTemplate.update(
                """
                INSERT INTO saas_usage_monthly
                    (id, organization_id, feature_code, period_start, quantity, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (organization_id, feature_code, period_start)
                DO UPDATE SET quantity = saas_usage_monthly.quantity + EXCLUDED.quantity,
                              updated_at = EXCLUDED.updated_at
                """,
                UUID.randomUUID(),
                organizationId,
                normalizedFeature,
                periodStart,
                quantity,
                Timestamp.from(now)
        );
        return true;
    }

    @Transactional(readOnly = true)
    public long currentMonth(
            Authentication authentication,
            UUID organizationId,
            String featureCode
    ) {
        tenantContextResolver.resolve(authentication, organizationId);
        validate(featureCode, 1, "usage-read");
        LocalDate periodStart = clock.instant().atZone(ZoneOffset.UTC)
                .toLocalDate().withDayOfMonth(1);
        Long quantity = jdbcTemplate.query(
                """
                SELECT quantity
                FROM saas_usage_monthly
                WHERE organization_id = ? AND feature_code = ? AND period_start = ?
                """,
                (resultSet, rowNum) -> resultSet.getLong("quantity"),
                organizationId,
                featureCode.trim().toUpperCase(java.util.Locale.ROOT),
                periodStart
        ).stream().findFirst().orElse(0L);
        return quantity == null ? 0L : quantity;
    }

    private void validate(String featureCode, long quantity, String idempotencyKey) {
        if (featureCode == null || !featureCode.matches("[A-Za-z0-9_.-]{1,80}")) {
            throw new IllegalArgumentException("featureCode inválido");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity deve ser maior que zero");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 255) {
            throw new IllegalArgumentException("idempotencyKey inválida");
        }
    }
}
