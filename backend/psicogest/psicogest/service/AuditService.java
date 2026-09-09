package com.psicogest.psicogest.service;

import com.psicogest.psicogest.model.entity.AuditLog;
import com.psicogest.psicogest.repository.AuditLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(
            AuditLogRepository auditLogRepository
    ) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(
            propagation = Propagation.MANDATORY
    )
    public AuditLog recordCriticalWrite(
            AuditCommand command
    ) {

        Instant now = Instant.now();

        AuditLog auditLog = AuditLog.builder()

                .id( UUID.randomUUID() )

                .userId( command.userId() )

                .sessionId(
                        command.sessionId()
                )

                .action( command.action() )

                .resourceType(
                        command.resourceType()
                )

                .resourceId(
                        command.resourceId()
                )

                .patientId(
                        command.patientId()
                )

                .description(
                        command.description()
                )

                .outcome( command.outcome() )

                .correlationId(
                        command.correlationId()
                )

                .sourceIp(
                        command.sourceIp()
                )

                .userAgentHash(
                        command.userAgentHash()
                )

                .metadata(
                        command.metadata()
                )

                .occurredAt( now )

                .createdAt( now )

                .build();

        AuditLog saved =
                auditLogRepository.save(
                        auditLog
                );

        log.debug(
                "Audit log registrado: auditId={}, action={}, resourceId={}, outcome={}",
                saved.getId(),
                command.action(),
                command.resourceId(),
                command.outcome()
        );

        return saved;
    }
}
