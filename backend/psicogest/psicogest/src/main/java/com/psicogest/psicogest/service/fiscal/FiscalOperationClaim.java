package com.psicogest.psicogest.service.fiscal;

import java.util.UUID;

import com.psicogest.psicogest.model.enums.FiscalProviderType;
import com.psicogest.psicogest.model.enums.FiscalOperationType;

public record FiscalOperationClaim(

    UUID operationId,

    FiscalProviderType provider,

    FiscalOperationType operationType,

    FiscalIssueCommand issueCommand,

    FiscalCancellationCommand cancellationCommand,

    FiscalSubstitutionCommand substitutionCommand

) {}
