package com.psicogest.psicogest.service.privacy;

import com.psicogest.psicogest.domain.privacy.AnonymizationResult;

public interface DataAnonymizer<T> {

    AnonymizationResult anonymize(
            T resource
    );
}
