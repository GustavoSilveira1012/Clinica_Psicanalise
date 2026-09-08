package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.AuditLog;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

public interface AuditLogRepository
        extends Repository<
                AuditLog,
                UUID
        > {

    AuditLog save(
            AuditLog auditLog
    );

    Optional<AuditLog> findById(
            UUID id
    );

    Optional<AuditLog>
    findBySequence(
            Long sequence
    );

    List<AuditLog>
    findBySequenceBetweenOrderBySequence(
            Long start,
            Long end
    );
}