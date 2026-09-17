package com.psicogest.psicogest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Catálogo mínimo para a operação; não expõe configurações financeiras sensíveis. */
public record PackagePlanCatalogResponse(
        UUID id,
        String name,
        String status,
        int sessions,
        BigDecimal price,
        long activeSubscriptions,
        Instant updatedAt
) {
}
