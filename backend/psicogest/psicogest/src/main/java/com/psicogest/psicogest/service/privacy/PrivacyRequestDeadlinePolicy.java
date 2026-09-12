package com.psicogest.psicogest.service.privacy;

import com.psicogest.psicogest.domain.privacy.PrivacyRegulatoryContext;
import com.psicogest.psicogest.model.enums.DataSubjectRequestType;

import java.time.Instant;

public interface PrivacyRequestDeadlinePolicy {

    Instant calculateDueAt(
            DataSubjectRequestType type,
            Instant receivedAt,
            PrivacyRegulatoryContext context
    );
}
