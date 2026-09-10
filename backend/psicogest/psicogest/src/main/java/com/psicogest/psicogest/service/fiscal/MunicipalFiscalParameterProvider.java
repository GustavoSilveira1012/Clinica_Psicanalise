package com.psicogest.psicogest.service.fiscal;

import java.time.LocalDate;

import com.psicogest.psicogest.model.entity.FiscalConfiguration;
import com.psicogest.psicogest.model.entity.FiscalIssuer;

public interface MunicipalFiscalParameterProvider {

    MunicipalFiscalParameters getParameters(
        FiscalIssuer issuer,
        FiscalConfiguration configuration,
        LocalDate competenceDate,
        String serviceCode
    );
}
