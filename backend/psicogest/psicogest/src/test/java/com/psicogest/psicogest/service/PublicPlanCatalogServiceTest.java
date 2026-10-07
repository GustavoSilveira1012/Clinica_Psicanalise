package com.psicogest.psicogest.service;

import com.psicogest.psicogest.model.entity.saas.SaasPlan;
import com.psicogest.psicogest.model.entity.saas.SaasPlanVersion;
import com.psicogest.psicogest.model.enums.SaasPlanVersionStatus;
import com.psicogest.psicogest.repository.SaasPlanEntitlementRepository;
import com.psicogest.psicogest.repository.SaasPlanVersionRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class PublicPlanCatalogServiceTest {
    private final SaasPlanVersionRepository versions = mock(SaasPlanVersionRepository.class);
    private final SaasPlanEntitlementRepository entitlements = mock(SaasPlanEntitlementRepository.class);

    @Test
    void keepsUnapprovedPricesPrivateWithoutReadingTheDatabase() {
        assertFalse(new PublicPlanCatalogService(versions, entitlements, false, true)
                .publicPlans().available());
        assertFalse(new PublicPlanCatalogService(versions, entitlements, true, false)
                .publicPlans().available());
        verifyNoInteractions(versions, entitlements);
    }

    @Test
    void showsOnlyLatestApprovedVersionPerPlan() {
        SaasPlan plan = SaasPlan.builder().code("CLINIC").name("Clínica").active(true).build();
        SaasPlanVersion newest = version(plan, 2, "39.00");
        SaasPlanVersion older = version(plan, 1, "29.00");
        when(versions.findActiveCatalogVersions(SaasPlanVersionStatus.PUBLISHED))
                .thenReturn(List.of(newest, older));
        when(entitlements.findAllByPlanVersionId(newest.getId())).thenReturn(List.of());

        var result = new PublicPlanCatalogService(versions, entitlements, true, true).publicPlans();

        assertTrue(result.available());
        assertEquals(1, result.plans().size());
        assertEquals(new BigDecimal("39.00"), result.plans().getFirst().monthlyPrice());
        verify(entitlements, never()).findAllByPlanVersionId(older.getId());
    }

    private static SaasPlanVersion version(SaasPlan plan, int number, String price) {
        return SaasPlanVersion.builder()
                .id(UUID.randomUUID())
                .plan(plan)
                .version(number)
                .status(SaasPlanVersionStatus.PUBLISHED)
                .monthlyPrice(new BigDecimal(price))
                .currency("BRL")
                .publicVisible(true)
                .build();
    }
}
