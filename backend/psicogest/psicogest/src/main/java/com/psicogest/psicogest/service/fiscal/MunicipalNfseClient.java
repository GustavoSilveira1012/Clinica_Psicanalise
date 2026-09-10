package com.psicogest.psicogest.service.fiscal;

import java.util.Optional;

import com.psicogest.psicogest.model.entity.FiscalConfiguration;

/**
 * Cliente HTTP para integração com provedores municipais de NFS-e
 * 
 * Interface para abstrair chamadas REST/SOAP específicas de cada prefeitura
 */
public interface MunicipalNfseClient {

    FiscalIssueResult issue(
        byte[] xml,
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
