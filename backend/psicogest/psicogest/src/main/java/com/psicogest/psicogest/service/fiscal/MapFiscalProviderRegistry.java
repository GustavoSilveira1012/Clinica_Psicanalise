package com.psicogest.psicogest.service.fiscal;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.psicogest.psicogest.exception.FiscalConfigurationException;
import com.psicogest.psicogest.model.enums.FiscalProviderType;

@Component
public class MapFiscalProviderRegistry implements FiscalProviderRegistry {

    private final Map<FiscalProviderType, FiscalProvider> providers;

    public MapFiscalProviderRegistry(List<FiscalProvider> implementations) {
        EnumMap<FiscalProviderType, FiscalProvider> map = new EnumMap<>(FiscalProviderType.class);
        for (FiscalProvider provider : implementations) {
            if (map.put(provider.type(), provider) != null) {
                throw new FiscalConfigurationException("Mais de um provedor fiscal configurado para " + provider.type());
            }
        }
        providers = Map.copyOf(map);
    }

    @Override
    public FiscalProvider get(FiscalProviderType type) {
        FiscalProvider provider = providers.get(type);
        if (provider == null) throw new FiscalConfigurationException("Provedor fiscal não configurado: " + type);
        return provider;
    }
}
