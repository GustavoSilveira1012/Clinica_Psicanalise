package com.psicogest.psicogest.model.enums;

/**
 * Política de preço para cancelamento de pacote
 * 
 * Define como o reembolso é calculado quando um pacote é cancelado
 */
public enum PackageCancellationPricingPolicy {

    /**
     * Cálculo proporcional (PRO_RATA)
     * 
     * O reembolso é calculado proporcionalmente ao consumo:
     * - Sessões disponíveis / total * preço
     * - Aplicado automaticamente
     * - Transparente e previsível
     * 
     * Exemplo:
     * - Pacote: 10 sessões, R$ 1000
     * - Consumidas: 3 sessões
     * - Reembolso: 7 / 10 * 1000 = R$ 700
     */
    PRO_RATA,

    /**
     * Revisão manual
     * 
     * Cancelamento requer revisão de um gerente/admin.
     * Permite aplicar políticas comerciais customizadas:
     * - Descontos
     * - Sem reembolso por taxa administrativa
     * - Políticas especiais para pacientes VIP
     * 
     * Reembolso é calculado manualmente durante a revisão
     */
    MANUAL_REVIEW
}
