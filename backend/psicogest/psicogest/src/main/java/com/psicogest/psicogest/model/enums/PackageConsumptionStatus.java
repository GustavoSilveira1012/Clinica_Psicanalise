package com.psicogest.psicogest.model.enums;

/**
 * Status de uma sessão consumida de um pacote
 */
public enum PackageConsumptionStatus {

    /**
     * Sessão foi consumida e está ativa
     */
    ACTIVE,

    /**
     * Sessão foi revertida (agendamento cancelado)
     */
    REVERSED
}
