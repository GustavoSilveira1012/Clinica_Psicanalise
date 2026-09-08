package com.psicogest.psicogest.security.audit;

import com.psicogest.psicogest.model.entity.AuditChainState;
import com.psicogest.psicogest.model.entity.AuditLog;
import com.psicogest.psicogest.repository.AuditChainStateRepository;
import com.psicogest.psicogest.repository.AuditLogRepository;
import com.psicogest.psicogest.repository.UserRepository;
import com.psicogest.psicogest.repository.UserSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditService {

    private static final short CHAIN_ID = 1;

    private final AuditChainStateRepository chainRepository;

    private final AuditLogRepository auditRepository;

    private final UserRepository userRepository;

    private final UserSessionRepository sessionRepository;

    private final AuditIntegrityService integrityService;

    private final AuditKeyProvider keyProvider;

    public AuditService(
            AuditChainStateRepository chainRepository,
            AuditLogRepository auditRepository,
            UserRepository userRepository,
            UserSessionRepository sessionRepository,
            AuditIntegrityService integrityService,
            AuditKeyProvider keyProvider
    ) {

        this.chainRepository = chainRepository;
        this.auditRepository = auditRepository;
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.integrityService = integrityService;
        this.keyProvider = keyProvider;
    }

    @Transactional
    public AuditLog log(
            AuditCommand command
    ) {

        return append(command);
    }

    /**
     * Escrita clínica crítica.
     *
     * <p>Exige que o chamador já possua uma transação ativa
     * ({@link Propagation#MANDATORY}). O {@link AuditLog} e a
     * operação clínica são persistidos na mesma transação:
     * se a auditoria falhar, tudo faz rollback — fail-secure.
     *
     * <pre>
     *   salvar prontuário
     *     └─ recordCriticalWrite  ← mesma TX
     *          └─ COMMIT (ou ROLLBACK se qualquer passo falhar)
     * </pre>
     */
    @Transactional(
            propagation = Propagation.MANDATORY
    )
    public AuditLog recordCriticalWrite(
            AuditCommand command
    ) {

        return append(command);
    }

    /**
     * Leitura sensível (prontuários, exames, etc.).
     *
     * <p>Sempre abre uma transação própria
     * ({@link Propagation#REQUIRES_NEW}). Se o subsistema de
     * auditoria estiver com falha, a exceção sobe ao chamador
     * antes de o conteúdo ser entregue — o sistema não vaza
     * dados silenciosamente.
     *
     * <pre>
     *   buscar prontuário
     *     └─ recordSensitiveRead  ← TX independente
     *          ├─ falhou → exceção → 503, conteúdo não entregue
     *          └─ OK     → retorna prontuário ao chamador
     * </pre>
     */
    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public AuditLog recordSensitiveRead(
            AuditCommand command
    ) {

        return append(command);
    }

    private AuditLog append(
            AuditCommand command
    ) {

        AuditChainState state =
                chainRepository.lockChain();

        long sequence =
                state.getLastSequence()
                        + 1;

        String keyId =
                keyProvider.currentKeyId();

        String metadataHash =
                integrityService.metadataHash(
                        command.metadata()
                );

        AuditLog audit =
                AuditLog.builder()

                        .id(
                                UUID.randomUUID()
                        )

                        .sequence(sequence)

                        .previousMac(
                                state.getLastMac()
                        )

                        .keyId(keyId)

                        .metadataHash(
                                metadataHash
                        )

                        .actorUser(
                                command.actorUserId() != null
                                        ? userRepository
                                                .getReferenceById(
                                                        command.actorUserId()
                                                )
                                        : null
                        )

                        .session(
                                command.sessionId() != null
                                        ? sessionRepository
                                                .getReferenceById(
                                                        command.sessionId()
                                                )
                                        : null
                        )

                        .action(
                                command.action()
                        )

                        .resourceType(
                                command.resourceType()
                        )

                        .resourceId(
                                command.resourceId()
                        )

                        .patientId(
                                command.patientId()
                        )

                        .clinicContextId(
                                command.clinicContextId()
                        )

                        .outcome(
                                command.outcome()
                        )

                        .occurredAt(
                                Instant.now()
                        )

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
                                command.metadata() != null
                                        ? command.metadata()
                                        : Map.of()
                        )

                        .build();

        String entryMac =
                integrityService.calculateMac(
                        audit,
                        keyProvider.currentKey()
                );

        audit.setEntryMac(entryMac);

        auditRepository.save(audit);

        state.setLastSequence(sequence);

        state.setLastMac(entryMac);

        state.setUpdatedAt(Instant.now());

        chainRepository.save(state);

        return audit;
    }
}
