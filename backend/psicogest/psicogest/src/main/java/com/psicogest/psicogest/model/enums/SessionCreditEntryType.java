package com.psicogest.psicogest.model.enums;

/**
 * Tipo de evento que afeta saldo de créditos de sessão
 */
public enum SessionCreditEntryType {

    /**
     * Ativação de pacote (CREDIT)
     * Quando um pacote comprado é ativado, créditos são adicionados
     */
    PACKAGE_ACTIVATION,

    /**
     * Consumo em agendamento (DEBIT)
     * Quando uma sessão é consumida, créditos são decrementados
     */
    APPOINTMENT_CONSUMPTION,

    /**
     * Reversão de consumo (CREDIT)
     * Quando um agendamento é cancelado, créditos são restaurados
     */
    CONSUMPTION_REVERSAL,

    /**
     * Expiração de pacote (DEBIT)
     * Quando créditos expiram, são removidos do saldo
     */
    EXPIRATION,

    /**
     * Cancelamento de pacote (DEBIT)
     * Quando um pacote é cancelado, créditos não utilizados são removidos
     */
    PACKAGE_CANCELLATION,

    /**
     * Ajuste manual (CREDIT ou DEBIT)
     * Requer permissão especial e motivo obrigatório
     */
    MANUAL_ADJUSTMENT
}
