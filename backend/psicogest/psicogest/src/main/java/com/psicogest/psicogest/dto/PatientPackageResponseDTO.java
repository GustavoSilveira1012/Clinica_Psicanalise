package com.psicogest.psicogest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.psicogest.psicogest.model.enums.PatientPackageStatus;

/**
 * DTO de resposta para pacote de sessão do paciente
 * 
 * Resumo de um pacote com saldo calculado
 * Não persiste availableSessions - é calculado do ledger
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PatientPackageResponseDTO(

    /**
     * ID do pacote
     */
    UUID id,

    /**
     * ID do paciente
     */
    Long patientId,

    /**
     * ID da entidade financeira (clínica)
     */
    Long financialEntityId,

    /**
     * ID do plano
     */
    UUID packagePlanId,

    /**
     * ID da versão do plano no momento da compra
     */
    UUID packagePlanVersionId,

    /**
     * Nome do plano
     */
    String packageName,

    /**
     * Status do pacote
     */
    PatientPackageStatus status,

    /**
     * Preço pago na compra
     */
    BigDecimal purchasePrice,

    /**
     * Total de sessões concedidas
     */
    Integer grantedSessions,

    /**
     * Sessões consumidas
     */
    Integer consumedSessions,

    /**
     * Sessões disponíveis (calculado: granted - consumed)
     */
    Integer availableSessions,

    /**
     * Data/hora da compra
     */
    Instant purchasedAt,

    /**
     * Data/hora da ativação
     */
    Instant activatedAt,

    /**
     * Data/hora de expiração
     */
    Instant expiresAt

) {}
