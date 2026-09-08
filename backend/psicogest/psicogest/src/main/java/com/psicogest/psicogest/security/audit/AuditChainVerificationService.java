package com.psicogest.psicogest.security.audit;

import com.psicogest.psicogest.model.entity.AuditLog;
import com.psicogest.psicogest.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AuditChainVerificationService {

    private final AuditLogRepository repository;

    private final AuditIntegrityService integrityService;

    private final AuditKeyProvider keyProvider;

    public AuditChainVerificationService(
            AuditLogRepository repository,
            AuditIntegrityService integrityService,
            AuditKeyProvider keyProvider
    ) {

        this.repository = repository;
        this.integrityService = integrityService;
        this.keyProvider = keyProvider;
    }

    public AuditVerificationResult verify(
            long start,
            long end
    ) {

        List<AuditLog> logs =
                repository
                        .findBySequenceBetweenOrderBySequence(
                                start,
                                end
                        );

        String expectedPrevious = null;

        Long previousSequence = null;

        for (AuditLog audit : logs) {

            /*
             * Detecta buracos.
             */
            if (
                    previousSequence != null
                    &&
                    audit.getSequence()
                            != previousSequence + 1
            ) {

                return AuditVerificationResult.failure(
                        audit.getSequence(),
                        "SEQUENCE_GAP"
                );
            }

            if (
                    expectedPrevious != null
                    &&
                    !MessageDigest.isEqual(
                            expectedPrevious.getBytes(
                                    StandardCharsets.US_ASCII
                            ),
                            value(
                                    audit.getPreviousMac()
                            ).getBytes(
                                    StandardCharsets.US_ASCII
                            )
                    )
            ) {

                return AuditVerificationResult.failure(
                        audit.getSequence(),
                        "CHAIN_BROKEN"
                );
            }

            String metadataHash =
                    integrityService.metadataHash(
                            audit.getMetadata()
                    );

            if (
                    !MessageDigest.isEqual(
                            metadataHash.getBytes(
                                    StandardCharsets.US_ASCII
                            ),
                            audit.getMetadataHash()
                                    .getBytes(
                                            StandardCharsets.US_ASCII
                                    )
                    )
            ) {

                return AuditVerificationResult.failure(
                        audit.getSequence(),
                        "METADATA_MODIFIED"
                );
            }

            String expectedMac =
                    integrityService.calculateMac(

                            audit,

                            keyProvider.keyFor(
                                    audit.getKeyId()
                            )
                    );

            if (
                    !MessageDigest.isEqual(
                            expectedMac.getBytes(
                                    StandardCharsets.US_ASCII
                            ),
                            audit.getEntryMac()
                                    .getBytes(
                                            StandardCharsets.US_ASCII
                                    )
                    )
            ) {

                return AuditVerificationResult.failure(
                        audit.getSequence(),
                        "MAC_INVALID"
                );
            }

            expectedPrevious =
                    audit.getEntryMac();

            previousSequence =
                    audit.getSequence();
        }

        return AuditVerificationResult.success(
                logs.size()
        );
    }

    private String value(Object v) {
        return v == null ? "" : v.toString();
    }
}