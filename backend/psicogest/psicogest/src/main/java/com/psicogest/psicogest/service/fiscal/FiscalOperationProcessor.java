package com.psicogest.psicogest.service.fiscal;

import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FiscalOperationProcessor {

    private final FiscalOperationService operationService;

    private final FiscalProviderRegistry providerRegistry;

    private final FiscalResultService resultService;

    public FiscalOperationProcessor(
            FiscalOperationService operationService,
            FiscalProviderRegistry providerRegistry,
            FiscalResultService resultService
    ) {
        this.operationService = operationService;
        this.providerRegistry = providerRegistry;
        this.resultService = resultService;
    }

    public void process(
        UUID operationId
    ) {

        FiscalOperationClaim claim =
            operationService.claim(
                operationId
            );

        if (claim == null) {
            return;
        }

        /*
         * Fora da transaction que fez o claim.
         */
        FiscalProvider provider = providerRegistry.get(claim.provider());
        FiscalIssueResult result;
        switch (claim.operationType()) {
            case ISSUE -> result = provider.issue(claim.issueCommand());
            case CANCEL -> {
                FiscalCancellationResult cancellation = provider.cancel(claim.cancellationCommand());
                result = new FiscalIssueResult(cancellation.success(), null, null, cancellation.errorMessage());
            }
            case SUBSTITUTE -> {
                FiscalSubstitutionResult substitution = provider.substitute(claim.substitutionCommand());
                result = new FiscalIssueResult(substitution.success(), substitution.newNfseId(), substitution.newAccessKey(), substitution.errorMessage());
            }
            default -> throw new IllegalStateException("Operação fiscal não suportada: " + claim.operationType());
        }

        resultService.apply(
            operationId,
            result
        );
    }
}
