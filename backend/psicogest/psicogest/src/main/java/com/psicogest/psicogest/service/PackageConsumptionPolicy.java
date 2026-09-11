package com.psicogest.psicogest.service;

import java.time.Instant;

import com.psicogest.psicogest.model.entity.Appointment;

/**
 * Política de consumo de créditos
 * 
 * Define QUANDO um crédito deve ser consumido
 * Abstrai as regras de negócio para não hardcodar if/else
 */
public interface PackageConsumptionPolicy {

    /**
     * Determina se um crédito deve ser consumido
     * 
     * @param appointment Agendamento que terminou
     * @param policyData Dados da política (configurações do plano)
     * @param eventTime Momento do evento
     * @return true se deve consumir, false caso contrário
     */
    boolean shouldConsume(
        Appointment appointment,
        Object policyData,
        Instant eventTime
    );
}
