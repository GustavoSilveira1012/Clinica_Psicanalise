package com.psicogest.psicogest.infrastructure.exfiltration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Detita acessos suspeitos a dados clínicos.
 * Registra padrões que podem indicar exfiltração.
 */
@Slf4j
@Service
public class ClinicalAccessDetector {

    /**
     * Registra acesso a dados clínicos para análise de exfiltração.
     *
     * Métricas monitoradas:
     * - Taxa de leitura por usuário/hora
     * - Origem geográfica suspeita
     * - Padrão de acesso a múltiplos pacientes
     */
    public void recordClinicalAccess(
            Long userId,
            UUID sessionId,
            Long patientId,
            String sourceIp
    ) {

        log.info(
                "Clinical data access: userId={}, sessionId={}, patientId={}, sourceIp={}",
                userId,
                sessionId,
                patientId,
                sourceIp
        );

        // TODO: Implementar detecção de anomalias
        // - Comparar com baseline histórico
        // - Alertar se suspeito
    }
}
