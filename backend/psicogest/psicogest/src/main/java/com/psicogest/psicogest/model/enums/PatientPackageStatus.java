package com.psicogest.psicogest.model.enums;

/**
 * Status de um pacote adquirido por paciente
 */
public enum PatientPackageStatus {

    /**
     * Pacote foi comprado mas não ativado ainda
     * Pode ser cancelado sem perda
     */
    PENDING_ACTIVATION,

    /**
     * Pacote está ativo e com sessões disponíveis
     */
    ACTIVE,

    /**
     * Pacote foi suspenso (ex: falta pagamento)
     */
    SUSPENDED,

    /**
     * Todas as sessões foram consumidas
     */
    EXHAUSTED,

    /**
     * Pacote expirou por data
     */
    EXPIRED,

    /**
     * Pacote foi cancelado
     */
    CANCELLED
}
