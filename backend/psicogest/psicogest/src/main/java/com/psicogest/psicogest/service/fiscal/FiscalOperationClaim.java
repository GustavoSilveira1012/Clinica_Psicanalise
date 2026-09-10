package com.psicogest.psicogest.service.fiscal;

import com.psicogest.psicogest.model.enums.FiscalProviderType;

public record FiscalOperationClaim(

    FiscalProviderType provider,

    FiscalIssueCommand command

) {}
