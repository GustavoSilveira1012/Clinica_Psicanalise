package com.psicogest.psicogest.service.fiscal;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.psicogest.psicogest.model.entity.FiscalConfiguration;
import com.psicogest.psicogest.model.enums.FiscalProviderType;

import lombok.extern.slf4j.Slf4j;

/**
 * Implementação do provedor fiscal nacional (ABRASF)
 * 
 * Sistema Nacional de NFS-e gerenciado pela SEFIN
 * Endpoints publicados em: https://www.gov.br/economia/pt-br/servicos/nfse-nacional
 */
@Slf4j
@Component
public class NationalNfseProvider
        implements FiscalProvider {

    private final NationalNfseClient client;

    private final DpsXmlGenerator xmlGenerator;

    private final FiscalXmlValidator xmlValidator;

    public NationalNfseProvider(
            NationalNfseClient client,
            DpsXmlGenerator xmlGenerator,
            FiscalXmlValidator xmlValidator
    ) {
        this.client = client;
        this.xmlGenerator = xmlGenerator;
        this.xmlValidator = xmlValidator;
    }

    @Override
    public FiscalProviderType type() {
        return FiscalProviderType.NATIONAL_NFSE;
    }

    /**
     * Emissão síncrona de NFS-e
     * 
     * 1. Gera DPS XML
     * 2. Valida XSD
     * 3. Envia ao provedor
     * 4. Retorna resultado com NFS-e ou rejeição
     */
    @Override
    public FiscalIssueResult issue(
        FiscalIssueCommand command
    ) {

        byte[] xml =
            xmlGenerator.generate(
                command
            );

        xmlValidator.validate(
            xml,
            command.layoutVersion()
        );

        log.info(
                "Emitindo NFS-e: dpsNumber={}, issuer={}",
                command.dpsNumber(),
                command.issuerId()
        );

        return client.issue(
            xml,
            command
        );
    }

    /**
     * Consulta NFS-e por chave de acesso
     */
    @Override
    public Optional<FiscalQueryResult> findByAccessKey(
        FiscalConfiguration configuration,
        String accessKey
    ) {

        log.info(
                "Consultando NFS-e por chave: {}",
                accessKey
        );

        return client.findByAccessKey(
            configuration,
            accessKey
        );
    }

    /**
     * Consulta NFS-e por DPS
     */
    @Override
    public Optional<FiscalQueryResult> findByDps(
        FiscalConfiguration configuration,
        DpsIdentifier dps
    ) {

        log.info(
                "Consultando NFS-e por DPS: {}",
                dps
        );

        return client.findByDps(
            configuration,
            dps
        );
    }

    /**
     * Cancelamento de NFS-e
     * 
     * Sistema Nacional permite cancelamento com justificativa
     */
    @Override
    public FiscalCancellationResult cancel(
        FiscalCancellationCommand command
    ) {

        log.info(
                "Cancelando NFS-e: nfseId={}",
                command.nfseId()
        );

        return client.cancel(command);
    }

    /**
     * Substituição de NFS-e
     * 
     * Gera nova NFS-e referenciando a original
     * e cancela automaticamente a original
     */
    @Override
    public FiscalSubstitutionResult substitute(
        FiscalSubstitutionCommand command
    ) {

        log.info(
                "Substituindo NFS-e: original={}, new={}",
                command.originalNfseId(),
                command.newDpsNumber()
        );

        return client.substitute(command);
    }
}
