package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.AuditLog;
import com.psicogest.psicogest.model.enums.AuditAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository append-only para AuditLog.
 * Usa Repository em vez de JpaRepository para
 * impedir delete/update acidentais.
 */
public interface AuditLogRepository
        extends Repository<AuditLog, UUID> {

    AuditLog save(
            AuditLog auditLog
    );

    List<AuditLog>
    findByUserIdAndOccurredAtAfterOrderByOccurredAtDesc(
            Long userId,
            Instant after
    );

    List<AuditLog>
    findByResourceIdAndActionOrderByOccurredAtDesc(
            String resourceId,
            AuditAction action
    );

    List<AuditLog>
    findByPatientIdAndOccurredAtAfterOrderByOccurredAtDesc(
            Long patientId,
            Instant after
    );

    List<AuditLog>
    findByActionOrderByOccurredAtDesc(
            AuditAction action
    );
}
