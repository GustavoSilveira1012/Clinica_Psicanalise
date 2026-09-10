package com.psicogest.psicogest.service.fiscal;

import java.util.UUID;

/**
 * Serviço de gerenciamento de operações fiscais
 * 
 * Responsável por:
 * - Reivindicar operações pendentes (claim)
 * - Controlar concorrência entre processadores
 * - Evitar processamento duplicado
 */
public interface FiscalOperationService {

    /**
     * Reclama uma operação pendente para processamento
     * 
     * Com lock pessimista para evitar race conditions
     */
    FiscalOperationClaim claim(UUID operationId);
}
