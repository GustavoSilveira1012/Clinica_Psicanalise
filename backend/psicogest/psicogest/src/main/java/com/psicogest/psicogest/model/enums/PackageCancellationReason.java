package com.psicogest.psicogest.model.enums;

/**
 * Motivo do cancelamento de pacote
 * 
 * Classifica o motivo pelo qual um pacote está sendo cancelado
 * para fins de auditoria e análise comercial
 */
public enum PackageCancellationReason {

    /**
     * Solicitação do paciente
     * 
     * Paciente solicitou o cancelamento por qualquer motivo
     * (quer seguir com outro terapeuta, fim da terapia, etc)
     */
    PATIENT_REQUEST,

    /**
     * Encerramento do relacionamento terapêutico
     * 
     * O relacionamento terapêutico foi finalizado
     * (alta clínica, abandono, etc)
     */
    THERAPEUTIC_RELATIONSHIP_ENDED,

    /**
     * Erro de cobrança
     * 
     * Cancelamento devido a erro administrativo:
     * - Cobrança incorreta
     * - Valores errados
     * - Configuração indevida
     */
    BILLING_ERROR,

    /**
     * Compra duplicada
     * 
     * Pacote foi comprado mais de uma vez (acidentalmente)
     * e está sendo cancelado por redundância
     */
    DUPLICATE_PURCHASE,

    /**
     * Outro motivo
     * 
     * Motivo não classificado nas categorias acima.
     * Deve ser acompanhado de descrição adicional.
     */
    OTHER
}
