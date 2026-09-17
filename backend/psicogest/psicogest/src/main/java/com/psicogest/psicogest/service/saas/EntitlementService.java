package com.psicogest.psicogest.service.saas;

import java.util.OptionalLong;
import java.util.UUID;

/** Contrato neutro para enforcement de recursos do plano no backend. */
public interface EntitlementService {

    boolean hasFeature(UUID organizationId, String feature);

    OptionalLong getLimit(UUID organizationId, String feature);

    void requireFeature(UUID organizationId, String feature);

    void requireCapacity(UUID organizationId, String metric, long requestedAmount);
}
