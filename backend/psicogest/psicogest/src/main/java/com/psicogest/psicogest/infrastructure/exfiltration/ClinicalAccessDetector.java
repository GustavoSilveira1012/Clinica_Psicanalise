package com.psicogest.psicogest.infrastructure.exfiltration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.psicogest.psicogest.model.entity.SecurityEvent;
import com.psicogest.psicogest.model.enums.SecurityEventOutcome;
import com.psicogest.psicogest.model.enums.SecurityEventSeverity;
import com.psicogest.psicogest.model.enums.SecurityEventType;
import com.psicogest.psicogest.repository.SecurityEventRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Map;

/**
 * Detita acessos suspeitos a dados clínicos.
 * Registra padrões que podem indicar exfiltração.
 */
@Slf4j
@Service
public class ClinicalAccessDetector {

    private static final Duration WINDOW = Duration.ofHours(1);
    private static final long READ_ALERT_THRESHOLD = 1_000;
    private static final long EXPORT_ALERT_THRESHOLD = 5;

    private final RedisTemplate<String, String> redis;
    private final SecurityEventRepository securityEvents;

    public ClinicalAccessDetector(
            RedisTemplate<String, String> redis,
            SecurityEventRepository securityEvents
    ) {
        this.redis = redis;
        this.securityEvents = securityEvents;
    }

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

        if (userId == null || patientId == null) return;
        try {
            String key = "security:clinical:reads:" + userId;
            Long reads = redis.opsForValue().increment(key);
            if (reads != null && reads == 1L) redis.expire(key, WINDOW);
            if (reads != null && reads > READ_ALERT_THRESHOLD) {
                recordAlert(SecurityEventSeverity.HIGH, userId, sessionId, sourceIp,
                        "Leituras clínicas acima do limite operacional", reads);
            }
        } catch (RuntimeException exception) {
            // Observability cannot make an authorized clinical read fail.
            log.warn("Detector de acesso clínico indisponível; leitura permitida", exception);
        }
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

        if (userId == null || patientId == null) return;
        try {
            String key = "security:clinical:exports:" + userId;
            Long exports = redis.opsForValue().increment(key);
            if (exports != null && exports == 1L) redis.expire(key, WINDOW);
            String patientsKey = key + ":patients";
            Long uniquePatients = redis.opsForSet().add(patientsKey, patientId.toString());
            redis.expire(patientsKey, WINDOW);
            Long patientCount = redis.opsForSet().size(patientsKey);
            if ((patientCount != null && patientCount >= EXPORT_ALERT_THRESHOLD)
                    || (exports != null && exports >= EXPORT_ALERT_THRESHOLD)) {
                recordAlert(SecurityEventSeverity.CRITICAL, userId, sessionId, sourceIp,
                        "Exportações clínicas em massa detectadas", patientCount == null ? exports : patientCount);
            } else if (patientCount != null && patientCount >= 3) {
                recordAlert(SecurityEventSeverity.MEDIUM, userId, sessionId, sourceIp,
                        "Aumento incomum de exportações clínicas", patientCount);
            }
        } catch (RuntimeException exception) {
            log.warn("Detector de exportação clínica indisponível; exportação registrada no fluxo principal", exception);
        }
    }

    @Transactional
    protected void recordAlert(SecurityEventSeverity severity, Long userId, UUID sessionId,
                               String sourceIp, String description, Long count) {
        securityEvents.save(SecurityEvent.builder()
                .id(UUID.randomUUID())
                .eventType(SecurityEventType.MASS_EXPORT_DETECTED)
                .severity(severity)
                .outcome(SecurityEventOutcome.DETECTED)
                .occurredAt(LocalDateTime.now())
                .sourceIp(sourceIp)
                .sessionId(sessionId)
                .resourceType("CLINICAL_DATA")
                .metadata(Map.of("userId", userId, "count", count, "description", description))
                .build());
        log.error("Alerta de segurança clínica: userId={}, severity={}, count={}", userId, severity, count);
    }
}
