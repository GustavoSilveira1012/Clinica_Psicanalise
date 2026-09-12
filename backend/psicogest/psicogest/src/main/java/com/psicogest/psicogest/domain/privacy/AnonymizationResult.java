package com.psicogest.psicogest.domain.privacy;

public record AnonymizationResult(
        boolean changed,
        String outcomeCode
) {
}
