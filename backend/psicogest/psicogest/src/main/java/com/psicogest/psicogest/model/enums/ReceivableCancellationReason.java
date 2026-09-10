package com.psicogest.psicogest.model.enums;

/**
 * Motivo do cancelamento de cobrança
 */
public enum ReceivableCancellationReason {
    /**
     * Cancelamento da consulta/serviço
     */
    SERVICE_CANCELLED,

    /**
     * Paciente solicitou cancelamento
     */
    PATIENT_REQUEST,

    /**
     * Erro administrativo
     */
    ADMINISTRATIVE_ERROR,

    /**
     * Outro motivo
     */
    OTHER
}
