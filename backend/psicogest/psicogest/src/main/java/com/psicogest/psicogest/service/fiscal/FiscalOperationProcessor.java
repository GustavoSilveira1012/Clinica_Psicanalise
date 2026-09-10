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
        FiscalIssueResult result =
            providerRegistry
                .get(claim.provider())
                .issue(
                    claim.command()
                );

        resultService.apply(
            operationId,
            result
        );
    }
}
