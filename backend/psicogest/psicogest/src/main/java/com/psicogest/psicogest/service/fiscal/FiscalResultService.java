package com.psicogest.psicogest.service.fiscal;

import java.util.UUID;

/**
 * Serviço de aplicação de resultados fiscais
 * 
 * Responsável por:
 * - Processar resultado do provedor
 * - Atualizar status da invoice
 * - Armazenar documentos gerados
 * - Auditoria de eventos fiscais
 */
public interface FiscalResultService {

    /**
     * Aplica resultado de operação fiscal
     */
    void apply(UUID operationId, FiscalIssueResult result);
}
