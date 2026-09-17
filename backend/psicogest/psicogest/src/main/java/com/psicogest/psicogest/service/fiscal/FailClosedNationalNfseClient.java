package com.psicogest.psicogest.service.fiscal;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.psicogest.psicogest.exception.FiscalConfigurationException;
import com.psicogest.psicogest.model.entity.FiscalConfiguration;

/**
 * Bean explícito para manter o sistema fail-closed até a homologação do
 * endpoint nacional. Nunca simula autorização fiscal nem retorna sucesso.
 */
@Component
public class FailClosedNationalNfseClient implements NationalNfseClient {

    private FiscalConfigurationException unavailable() {
        return new FiscalConfigurationException(
                "Integração NFS-e nacional não homologada; configure o cliente fiscal antes de emitir");
    }

    @Override public FiscalIssueResult issue(byte[] xml, FiscalIssueCommand command) { throw unavailable(); }
    @Override public Optional<FiscalQueryResult> findByAccessKey(FiscalConfiguration configuration, String accessKey) { throw unavailable(); }
    @Override public Optional<FiscalQueryResult> findByDps(FiscalConfiguration configuration, DpsIdentifier dps) { throw unavailable(); }
    @Override public FiscalCancellationResult cancel(FiscalCancellationCommand command) { throw unavailable(); }
    @Override public FiscalSubstitutionResult substitute(FiscalSubstitutionCommand command) { throw unavailable(); }
}
