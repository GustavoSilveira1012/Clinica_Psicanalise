package com.psicogest.psicogest.infrastructure.payment.provider.secret;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.psicogest.psicogest.infrastructure.payment.provider.PaymentProviderType;

import lombok.extern.slf4j.Slf4j;

/**
 * 42. Implementação PROD: segredos via HashiCorp Vault / AWS Secrets Manager
 * 
 * Stub por enquanto. Implementação completa:
 * 1. Integrar Spring Vault
 * 2. Configurar VaultTemplate
 * 3. Ler paths: secret/payment/{PROVIDER}/api-key, secret/payment/{PROVIDER}/webhook-secret
 * 
 * Exemplo com Spring Vault:
 * VaultOperations vault = vaultTemplate.opsForKeyValue("secret");
 * VaultKeyValueOperations kv = vault.get("payment/" + provider.name());
 * String apiKey = kv.get("api-key");
 */
@Slf4j
@Component
@Profile("prod")
public class VaultPaymentProviderSecretProvider
        implements PaymentProviderSecretProvider {

    // TODO: Injetar VaultTemplate quando Spring Vault for adicionado
    // private final VaultTemplate vaultTemplate;

    @Override
    public String apiCredential(PaymentProviderType provider) {

        // TODO: Implementar leitura do Vault
        throw new UnsupportedOperationException(
                "VaultPaymentProviderSecretProvider ainda não implementado. " +
                        "Adicionar Spring Vault ao pom.xml e configurar."
        );
    }

    @Override
    public String webhookSecret(PaymentProviderType provider) {

        // TODO: Implementar leitura do Vault
        throw new UnsupportedOperationException(
                "VaultPaymentProviderSecretProvider ainda não implementado. " +
                        "Adicionar Spring Vault ao pom.xml e configurar."
        );
    }
}
