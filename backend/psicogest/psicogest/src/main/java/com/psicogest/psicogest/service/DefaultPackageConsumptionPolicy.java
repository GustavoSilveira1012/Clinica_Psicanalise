package com.psicogest.psicogest.service;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.psicogest.psicogest.model.entity.Appointment;
import com.psicogest.psicogest.model.enums.AppointmentStatus;

import lombok.extern.slf4j.Slf4j;

/**
 * Política padrão de consumo de créditos
 * 
 * Regra inicial:
 * - COMPLETED → consome 1 crédito
 * 
 * Políticas opcionais (futuro):
 * - NO_SHOW → pode consumir (conforme política)
 * - CANCELLED_LATE → pode consumir (conforme política)
 */
@Slf4j
@Component
public class DefaultPackageConsumptionPolicy
        implements PackageConsumptionPolicy {

    @Override
    public boolean shouldConsume(
            Appointment appointment,
            Object policyData,
            Instant eventTime
    ) {

        // Regra básica: COMPLETED sempre consome
        if (appointment.getStatus() ==
                AppointmentStatus.COMPLETED) {

            log.debug(
                    "Política: COMPLETED → consumir crédito"
            );

            return true;
        }

        log.debug(
                "Política: {} → NÃO consumir",
                appointment.getStatus()
        );

        return false;
    }
}

