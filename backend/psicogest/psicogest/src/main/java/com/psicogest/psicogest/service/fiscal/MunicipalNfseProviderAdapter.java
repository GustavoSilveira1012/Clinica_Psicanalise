package com.psicogest.psicogest.service.fiscal;

import java.util.Optional;

import com.psicogest.psicogest.model.entity.FiscalConfiguration;
import com.psicogest.psicogest.model.enums.FiscalProviderType;

import lombok.extern.slf4j.Slf4j;

/**
 * Adapter base para provedores municipais de NFS-e
 * 
 * Cada prefeitura pode ter particularidades:
 * - Endpoint diferente
 * - Formato XML customizado
 * - Regras de validação específicas
 * - Processo de autenticação próprio
 * 
 * Estender esta classe para implementar particularidades municipais
 */
@Slf4j
public abstract class MunicipalNfseProviderAdapter
        implements FiscalProvider {

    protected final MunicipalNfseClient client;

    protected final DpsXmlGenerator xmlGenerator;

    protected final FiscalXmlValidator xmlValidator;

    public MunicipalNfseProviderAdapter(
            MunicipalNfseClient client,
            DpsXmlGenerator xmlGenerator,
            FiscalXmlValidator xmlValidator
    ) {
        this.client = client;
        this.xmlGenerator = xmlGenerator;
        this.xmlValidator = xmlValidator;
    }

    @Override
    public FiscalProviderType type() {
        return FiscalProviderType.MUNICIPAL;
    }

    /**
     * Template Method: emissão com hook para customização municipal
     */
    @Override
    public FiscalIssueResult issue(
        FiscalIssueCommand command
    ) {

        // Validações pré-emissão (pode ser override)
        validatePreIssue(command);

        // Gera DPS
        byte[] xml =
            xmlGenerator.generate(
                command
            );

        // Valida XSD
        xmlValidator.validate(
            xml,
            command.layoutVersion()
        );

        // Hook: transformação municipal específica
        byte[] municipalXml =
            transformToMunicipalFormat(xml, command);

        log.info(
                "Emitindo NFS-e municipal: dpsNumber={}, municipality={}",
                command.dpsNumber(),
                getMunicipalityCode()
        );

        return client.issue(
            municipalXml,
            command
        );
    }

    @Override
    public Optional<FiscalQueryResult> findByAccessKey(
        FiscalConfiguration configuration,
        String accessKey
    ) {

        log.info(
                "Consultando NFS-e municipal por chave: {}",
                accessKey
        );

        return client.findByAccessKey(
            configuration,
            accessKey
        );
    }

    @Override
    public Optional<FiscalQueryResult> findByDps(
        FiscalConfiguration configuration,
        DpsIdentifier dps
    ) {

        log.info(
                "Consultando NFS-e municipal por DPS: {}",
                dps
        );

        return client.findByDps(
            configuration,
            dps
        );
    }

    @Override
    public FiscalCancellationResult cancel(
        FiscalCancellationCommand command
    ) {

        log.info(
                "Cancelando NFS-e municipal: nfseId={}",
                command.nfseId()
        );

        return client.cancel(command);
    }

    @Override
    public FiscalSubstitutionResult substitute(
        FiscalSubstitutionCommand command
    ) {

        log.info(
                "Substituindo NFS-e municipal: original={}, new={}",
                command.originalNfseId(),
                command.newDpsNumber()
        );

        return client.substitute(command);
    }

    /**
     * Hook: validações pré-emissão customizáveis
     * Override em adapters municipais específicos
     */
    protected void validatePreIssue(
            FiscalIssueCommand command
    ) {
        // Default: sem validações adicionais
    }

    /**
     * Hook: transformação do XML para formato municipal
     * Override em adapters municipais específicos
     */
    protected byte[] transformToMunicipalFormat(
            byte[] standardXml,
            FiscalIssueCommand command
    ) {
        // Default: retorna XML padrão
        return standardXml;
    }

    /**
     * Deve retornar código IBGE ou identificação da prefeitura
     */
    protected abstract String getMunicipalityCode();
}
