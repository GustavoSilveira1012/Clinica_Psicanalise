package com.psicogest.psicogest.security.audit;

import java.time.Instant;

/**
 * Ponto de ancoragem imutável do log de auditoria.
 *
 * <p>Representa um snapshot de integridade em um determinado número de
 * sequência. Pode ser publicado em qualquer sink de armazenamento
 * imutável (S3 Object Lock, Azure Immutable Blob, Google Bucket Lock,
 * SIEM etc.) sem acoplar o domínio à nuvem.
 *
 * <p>Em produção, o campo {@code mac} pode ser substituído por uma
 * assinatura assimétrica gerada via KMS/HSM para garantia adicional
 * de não-repúdio.
 *
 * @param sequence  Número de sequência do último registro auditado
 *                  neste checkpoint.
 * @param mac       MAC (ou assinatura) do registro de sequência
 *                  correspondente, servindo de âncora criptográfica.
 * @param createdAt Instante em que o checkpoint foi gerado.
 */
public record AuditCheckpoint(

        long sequence,

        String mac,

        Instant createdAt

) {
}
