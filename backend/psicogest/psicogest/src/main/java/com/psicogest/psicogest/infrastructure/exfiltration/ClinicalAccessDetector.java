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

    /**
     * 26. Registra exportação clínica para detecção de MASS_EXPORT
     *
     * Uma exportação vale risco maior que simplesmente abrir um prontuário.
     *
     * Exemplos:
     * - 1 export → normal
     * - 3 pacientes diferentes em poucos minutos → MEDIUM/HIGH
     * - 5+ → MASS_EXPORT
     */
    public void recordClinicalExport(
            Long userId,
            UUID sessionId,
            Long patientId,
            String sourceIp
    ) {

        log.warn(
                "Clinical export: userId={}, sessionId={}, patientId={}, sourceIp={}",
                userId,
                sessionId,
                patientId,
                sourceIp
        );

        // TODO: Implementar detecção de MASS_EXPORT
        // - Rastrear exportações por usuário em janela de tempo
        // - Detectar múltiplos pacientes
        // - Escalar se padrão de exfiltração
    }
}
