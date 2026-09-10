package com.psicogest.psicogest.service.fiscal;

import java.util.Optional;

import com.psicogest.psicogest.model.entity.FiscalConfiguration;

/**
 * Cliente HTTP para integração com Sistema Nacional NFS-e (ABRASF)
 * 
 * Interface para abstrair chamadas REST/SOAP ao provedor
 */
public interface NationalNfseClient {

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
