package com.psicogest.psicogest.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * Request para criar assinatura de paciente
 * 
 * Dados enviados pelo frontend:
 * - subscriptionPlanVersionId: UUID da versão publicada do plano
 * - startsOn: data de início da assinatura
 * 
 * Dados NÃO enviados pelo frontend (vêm da versão):
 * - price: vem de SubscriptionPlanVersion.price ❌
 * - sessions: vem de SubscriptionPlanVersion.sessionsPerBillingCycle ❌
 * - financialEntityId: vem de SubscriptionPlan.financialEntity ❌
 * - nextBillingDate: calculado como startsOn + billingCycleDays ❌
 * 
 * Frontend não pode:
 * - Manipular preço
 * - Alterar quantidade de sessões
 * - Escolher entidade financeira arbitrariamente
 * 
 * O servidor sempre valida contra a versão publicada.
 */
public record CreateSubscriptionRequest(

        /**
         * ID da versão do plano de assinatura
         * 
         * Obrigatório. Deve ser uma versão publicada.
         * Lookup realizado no banco de dados.
         */
        @NotNull(message = "ID da versão do plano é obrigatório")
        UUID subscriptionPlanVersionId,

        /**
         * Data de início da assinatura
         * 
         * Obrigatório. Não pode ser retroativa.
         * Pode ser hoje ou data futura.
         */
        @NotNull(message = "Data de início é obrigatória")
        LocalDate startsOn

) {}
