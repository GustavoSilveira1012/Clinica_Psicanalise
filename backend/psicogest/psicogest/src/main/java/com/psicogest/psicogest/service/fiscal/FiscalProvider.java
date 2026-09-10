package com.psicogest.psicogest.service.fiscal;

import java.util.Optional;

import com.psicogest.psicogest.model.entity.FiscalConfiguration;
import com.psicogest.psicogest.model.enums.FiscalProviderType;

public interface FiscalProvider {

    FiscalProviderType type();

    FiscalIssueResult issue(
        FiscalIssueCommand command
    );

    Optional<FiscalQueryResult> findByAccessKey(
        FiscalConfiguration configuration,
        String accessKey
    );

    Optional<FiscalQueryResult> findByDps(
        FiscalConfiguration configuration,
        DpsIdentifier dps
    );

    FiscalCancellationResult cancel(
        FiscalCancellationCommand command
    );

    FiscalSubstitutionResult substitute(
        FiscalSubstitutionCommand command
    );
}
