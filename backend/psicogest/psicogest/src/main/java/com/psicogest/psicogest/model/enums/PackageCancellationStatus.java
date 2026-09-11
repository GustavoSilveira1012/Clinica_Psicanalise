package com.psicogest.psicogest.model.enums;

/**
 * Status do cancelamento de pacote
 * 
 * Representa os estados por que passa um cancelamento durante seu ciclo de vida
 */
public enum PackageCancellationStatus {

    /**
     * Cálculo realizado
     * 
     * Cancelamento foi solicitado, cálculos foram realizados:
     * - Sessões consumidas / disponíveis
     * - Valor a devolver / converter em crédito
     * - Política de pricing (PRO_RATA, etc)
     * 
     * Aguardando aprovação e settling
     */
    CALCULATED,

    /**
     * Liquidação em progresso
     * 
     * O processo de refund ou crédito está sendo executado.
     * Pode envolver:
     * - Criação de refund request
     * - Reversão de allocations
     * - Geração de crédito
     */
    SETTLING,

    /**
     * Cancelamento completo
     * 
     * Pacote foi cancelado e todas as operações foram concluídas:
     * - Refund processado OU
     * - Crédito criado
     * - Status do pacote alterado para CANCELLED
     */
    COMPLETED,

    /**
     * Falha no cancelamento
     * 
     * Erro durante a liquidação (ex: falha ao criar refund).
     * Pacote permanece ativo e pode ser retentado.
     */
    FAILED
}
