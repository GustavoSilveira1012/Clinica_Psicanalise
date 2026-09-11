package com.psicogest.psicogest.dto;

import jakarta.validation.constraints.NotNull;

import com.psicogest.psicogest.model.enums.PackageCancellationReason;
import com.psicogest.psicogest.model.enums.PackageCancellationSettlementMode;

/**
 * Request para cancelamento de pacote de sessão
 * 
 * Dados enviados pelo frontend para iniciar cancelamento.
 * 
 * Importante: Frontend NÃO envia:
 * - refundAmount: servidor calcula
 * - consumedValue: servidor calcula
 * - remainingValue: servidor calcula
 * - paidAmount: servidor consulta na cobrança
 * 
 * O servidor SEMPRE recalcula tudo dentro de locks
 * para garantir consistência e prevenir manipulação.
 * 
 * Idempotência:
 * - Use Idempotency-Key header com UUID único
 * - Mesmo request com mesma chave retorna mesmo resultado
 * - Previne duplicação acidental de cancelamentos
 */
public record CancelPatientPackageRequest(

        /**
         * Motivo do cancelamento
         * 
         * Obrigatório. Classificação comercial:
         * - PATIENT_REQUEST
         * - THERAPEUTIC_RELATIONSHIP_ENDED
         * - BILLING_ERROR
         * - DUPLICATE_PURCHASE
         * - OTHER
         */
        @NotNull(message = "Motivo do cancelamento é obrigatório")
        PackageCancellationReason reason,

        /**
         * Modo de liquidação
         * 
         * Opcional. Se não fornecido, usa default da política.
         * 
         * - NONE: sem devolução
         * - REFUND: devolver valor
         * - CREDIT_BALANCE: converter em crédito
         */
        PackageCancellationSettlementMode settlementMode

) {}
