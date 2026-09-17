package com.psicogest.psicogest.service.saas;

import com.psicogest.psicogest.dto.saas.EntitlementResponse;
import com.psicogest.psicogest.exception.FeatureNotEntitledException;
import com.psicogest.psicogest.exception.ResourceNotFoundException;
import com.psicogest.psicogest.exception.SaasCapacityExceededException;
import com.psicogest.psicogest.model.entity.saas.SaasPlanEntitlement;
import com.psicogest.psicogest.model.entity.saas.SaasSubscription;
import com.psicogest.psicogest.repository.SaasPlanEntitlementRepository;
import com.psicogest.psicogest.repository.SaasSubscriptionRepository;
import com.psicogest.psicogest.security.tenant.TenantContextResolver;
import com.psicogest.psicogest.security.tenant.TenantContext;
import com.psicogest.psicogest.security.tenant.TenantContextHolder;
import com.psicogest.psicogest.exception.TenantContextRequiredException;
import com.psicogest.psicogest.repository.SaasUsageMonthlyRepository;
import org.springframework.security.core.Authentication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.OptionalLong;
import java.util.UUID;

/** Backend enforcement for SaaS features; historical data is never hidden. */
@Service
public class SaasEntitlementService implements EntitlementService {

    private final TenantContextResolver tenantContextResolver;
    private final SaasSubscriptionRepository subscriptionRepository;
    private final SaasPlanEntitlementRepository entitlementRepository;
    private final SaasUsageMonthlyRepository usageMonthlyRepository;
    private final Clock clock;
    private final JdbcTemplate jdbcTemplate;

    public SaasEntitlementService(
            TenantContextResolver tenantContextResolver,
            SaasSubscriptionRepository subscriptionRepository,
            SaasPlanEntitlementRepository entitlementRepository,
            SaasUsageMonthlyRepository usageMonthlyRepository,
            Clock clock,
            JdbcTemplate jdbcTemplate
    ) {
        this.tenantContextResolver = tenantContextResolver;
        this.subscriptionRepository = subscriptionRepository;
        this.entitlementRepository = entitlementRepository;
        this.usageMonthlyRepository = usageMonthlyRepository;
        this.clock = clock;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasFeature(UUID organizationId, String featureCode) {
        return directEntitlement(organizationId, featureCode)
                .map(item -> Boolean.TRUE.equals(item.getEnabled()))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public OptionalLong getLimit(UUID organizationId, String featureCode) {
        return directEntitlement(organizationId, featureCode)
                .map(SaasPlanEntitlement::getLimitValue)
                .filter(java.util.Objects::nonNull)
                .map(OptionalLong::of)
                .orElseGet(OptionalLong::empty);
    }

    @Override
    @Transactional(readOnly = true)
    public void requireFeature(UUID organizationId, String featureCode) {
        SaasPlanEntitlement item = directEntitlement(organizationId, featureCode)
                .orElseThrow(() -> new FeatureNotEntitledException(
                        "O plano atual não inclui o recurso " + featureCode));
        if (!Boolean.TRUE.equals(item.getEnabled())) {
            throw new FeatureNotEntitledException(
                    "O plano atual não inclui o recurso " + featureCode);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void requireCapacity(UUID organizationId, String metric, long requestedAmount) {
        if (requestedAmount <= 0) {
            throw new IllegalArgumentException("requestedAmount deve ser maior que zero");
        }
        requireFeature(organizationId, metric);
        // O chamador deve manter esta transação aberta até a criação do recurso.
        // Assim, duas requisições concorrentes não passam pelo mesmo limite lógico.
        jdbcTemplateLock(organizationId, metric);
        OptionalLong limit = getLimit(organizationId, metric);
        if (limit.isEmpty()) {
            return;
        }
        LocalDate period = clock.instant().atZone(ZoneOffset.UTC).toLocalDate().withDayOfMonth(1);
        long current = usageMonthlyRepository
                .findByOrganizationIdAndFeatureCodeAndPeriodStart(organizationId, metric, period)
                .map(item -> item.getQuantity() == null ? 0L : item.getQuantity())
                .orElse(0L);
        if (current > limit.getAsLong() || requestedAmount > limit.getAsLong() - current) {
            throw new SaasCapacityExceededException(
                    "Limite do plano atingido para o recurso " + metric);
        }
    }

    @Transactional(readOnly = true)
    public boolean hasFeature(Authentication authentication, UUID organizationId, String featureCode) {
        return findEntitlement(authentication, organizationId, featureCode)
                .map(entitlement -> Boolean.TRUE.equals(entitlement.getEnabled()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public Long getLimit(Authentication authentication, UUID organizationId, String featureCode) {
        return findEntitlement(authentication, organizationId, featureCode)
                .map(SaasPlanEntitlement::getLimitValue)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public void requireFeature(Authentication authentication, UUID organizationId, String featureCode) {
        SaasPlanEntitlement entitlement = findEntitlement(authentication, organizationId, featureCode)
                .orElseThrow(() -> new FeatureNotEntitledException(
                        "O plano atual não inclui o recurso " + featureCode));
        if (!Boolean.TRUE.equals(entitlement.getEnabled())) {
            throw new FeatureNotEntitledException(
                    "O plano atual não inclui o recurso " + featureCode);
        }
    }

    @Transactional(readOnly = true)
    public void requireCapacity(
            Authentication authentication,
            UUID organizationId,
            String featureCode,
            long currentUsage,
            long requestedQuantity
    ) {
        requireFeature(authentication, organizationId, featureCode);
        Long limit = getLimit(authentication, organizationId, featureCode);
        if (limit != null && (currentUsage > limit || requestedQuantity > limit - currentUsage)) {
            throw new SaasCapacityExceededException(
                    "Limite do plano atingido para o recurso " + featureCode);
        }
    }

    @Transactional(readOnly = true)
    public List<EntitlementResponse> list(Authentication authentication, UUID organizationId) {
        SaasSubscription subscription = subscription(authentication, organizationId);
        return entitlementRepository.findAllByPlanVersionId(subscription.getPlanVersion().getId())
                .stream()
                .map(item -> new EntitlementResponse(
                        item.getFeature().getCode(),
                        Boolean.TRUE.equals(item.getEnabled()),
                        item.getLimitValue()))
                .toList();
    }

    private java.util.Optional<SaasPlanEntitlement> findEntitlement(
            Authentication authentication, UUID organizationId, String featureCode) {
        tenantContextResolver.resolve(authentication, organizationId);
        SaasSubscription subscription = subscriptionRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura SaaS não encontrada"));
        return entitlementRepository.findByPlanVersionIdAndFeatureCode(
                subscription.getPlanVersion().getId(), featureCode);
    }

    private java.util.Optional<SaasPlanEntitlement> directEntitlement(
            UUID organizationId,
            String featureCode
    ) {
        TenantContext context = TenantContextHolder.get();
        if (organizationId == null || context == null || !organizationId.equals(context.organizationId())) {
            throw new TenantContextRequiredException(
                    "Contexto da organização não foi estabelecido");
        }
        SaasSubscription subscription = subscriptionRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura SaaS não encontrada"));
        return entitlementRepository.findByPlanVersionIdAndFeatureCode(
                subscription.getPlanVersion().getId(), featureCode);
    }

    private void jdbcTemplateLock(UUID organizationId, String featureCode) {
        // pg_advisory_xact_lock é liberado automaticamente no commit/rollback.
        // A chave é derivada apenas de identificadores técnicos, sem dados clínicos.
        jdbcTemplate.queryForObject(
                "SELECT pg_advisory_xact_lock(hashtextextended(?, 0))",
                Object.class,
                organizationId + ":" + featureCode.trim().toUpperCase(java.util.Locale.ROOT));
    }

    private SaasSubscription subscription(Authentication authentication, UUID organizationId) {
        tenantContextResolver.resolve(authentication, organizationId);
        return subscriptionRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura SaaS não encontrada"));
    }
}
