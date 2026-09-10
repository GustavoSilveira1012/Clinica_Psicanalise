package com.psicogest.psicogest.service.fiscal;

import com.psicogest.psicogest.model.enums.FiscalProviderType;

public interface FiscalCredentialProvider {

    FiscalCredentialProvider resolve(
        String credentialReference,
        FiscalProviderType provider
    );
}
