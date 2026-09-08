package com.psicogest.psicogest.security.audit;

/**
 * Porta de saída para publicação de checkpoints de auditoria.
 *
 * <p>Desacopla o domínio de qualquer infraestrutura de armazenamento
 * imutável. Implementações possíveis:
 * <ul>
 *   <li>S3 Object Lock</li>
 *   <li>Azure Immutable Blob Storage</li>
 *   <li>Google Cloud Storage Bucket Lock</li>
 *   <li>SIEM (Splunk, Elastic SIEM etc.)</li>
 *   <li>Ledger / Blockchain permissionado</li>
 * </ul>
 *
 * <p>Em produção, o {@link AuditCheckpoint#mac()} pode carregar uma
 * assinatura assimétrica gerada via KMS/HSM para garantir
 * não-repúdio.
 */
public interface AuditCheckpointSink {

    /**
     * Publica um checkpoint de integridade no sink configurado.
     *
     * <p>Implementações devem ser idempotentes: publicar o mesmo
     * checkpoint duas vezes não deve causar duplicação ou erro.
     *
     * @param checkpoint o checkpoint a ser publicado; nunca {@code null}
     */
    void publish(
            AuditCheckpoint checkpoint
    );
}
