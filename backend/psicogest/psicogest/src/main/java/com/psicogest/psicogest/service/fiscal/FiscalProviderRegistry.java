package com.psicogest.psicogest.service.fiscal;

import com.psicogest.psicogest.model.enums.FiscalProviderType;

/**
 * Registry para descoberta dinâmica de provedores fiscais
 */
public interface FiscalProviderRegistry {

    FiscalProvider get(FiscalProviderType type);
}
