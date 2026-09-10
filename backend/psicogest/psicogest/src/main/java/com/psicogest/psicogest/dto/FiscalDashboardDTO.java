package com.psicogest.psicogest.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Data;

/**
 * Dashboard analytics para integração fiscal
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FiscalDashboardDTO {

    /**
     * Total de notas autorizadas
     */
    private Long authorizedCount;

    /**
     * Total de notas rejeitadas
     */
    private Long rejectedCount;

    /**
     * Soma de valores líquidos autorizados
     */
    private BigDecimal totalAuthorizedAmount;

    /**
     * Soma de valores brutos em período
     */
    private BigDecimal totalGrossAmount;

    /**
     * Soma de deduções (INSS, IRRF, PIS, COFINS) em período
     */
    private BigDecimal totalDeductions;

    /**
     * Taxa de rejeição (%)
     */
    private BigDecimal rejectionRate;

    /**
     * Saldo pendente de reconciliação
     */
    private BigDecimal pendingReconciliation;

    /**
     * Última atualização
     */
    private String lastUpdated;
}
