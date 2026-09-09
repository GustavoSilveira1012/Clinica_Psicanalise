package com.psicogest.psicogest.security.authorization;

import com.psicogest.psicogest.model.entity.Psychoanalyst;
import com.psicogest.psicogest.model.enums.MedicalRecordStatus;
import com.psicogest.psicogest.repository.MedicalRecordRepository;
import com.psicogest.psicogest.repository.PsychoanalystRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class ClinicalAuthorizationService {

    private final MedicalRecordRepository medicalRecordRepository;

    private final PsychoanalystRepository psychoanalystRepository;

    private final SecurityContextService contextService;

    public ClinicalAuthorizationService(
            MedicalRecordRepository medicalRecordRepository,
            PsychoanalystRepository psychoanalystRepository,
            SecurityContextService contextService
    ) {

        this.medicalRecordRepository = medicalRecordRepository;
        this.psychoanalystRepository = psychoanalystRepository;
        this.contextService = contextService;
    }

    /**
     * 26. Autorização para leitura de prontuário
     *
     * Autoriza se:
     * - Usuário tem role PSYCHOANALYST
     * - Usuário é o autor original do prontuário OU
     * - Usuário tem vínculo ACTIVE ou SUSPENDED com o paciente
     */
    public boolean canReadMedicalRecord(
            Authentication authentication,
            UUID medicalRecordId
    ) {

        if (
                !contextService.hasRole(
                        authentication,
                        "PSYCHOANALYST"
                )
        ) {

            log.debug(
                    "Acesso negado: usuário não tem role PSYCHOANALYST"
            );
            return false;
        }

        Long userId =
                contextService
                        .userId( authentication )
                        .orElse( null );

        if ( userId == null ) {

            log.debug(
                    "Acesso negado: userId não encontrado"
            );
            return false;
        }

        Psychoanalyst psychoanalyst =
                psychoanalystRepository
                        .findByUserId( userId )
                        .orElse( null );

        if ( psychoanalyst == null ) {

            log.debug(
                    "Acesso negado: usuário não é psicanalista"
            );
            return false;
        }

        boolean canRead =
                medicalRecordRepository
                        .existsReadableBy(
                                medicalRecordId,
                                psychoanalyst.getId()
                        );

        if ( !canRead ) {

            log.warn(
                    "Tentativa de leitura não autorizada: recordId={}, psychoanalystId={}",
                    medicalRecordId,
                    psychoanalyst.getId()
            );
        }

        return canRead;
    }

    /**
     * 43. Autorização para editar prontuário
     *
     * Permite edição apenas se:
     * - Usuário é o autor original
     * - Prontuário está em status DRAFT
     */
    public boolean canEditMedicalRecord(
            Authentication authentication,
            UUID medicalRecordId
    ) {

        Long userId = contextService
                .userId(authentication)
                .orElse(null);

        if (userId == null) {
            log.debug("Acesso negado: userId não encontrado");
            return false;
        }

        if (!contextService.hasRole(authentication, "PSYCHOANALYST")) {
            log.debug("Acesso negado: usuário não tem role PSYCHOANALYST");
            return false;
        }

        return psychoanalystRepository
                .findByUserId(userId)
                .map(psychoanalyst ->
                        medicalRecordRepository
                                .existsByIdAndAuthorPsychoanalystIdAndStatus(
                                        medicalRecordId,
                                        psychoanalyst.getId(),
                                        com.psicogest.psicogest.model.enums.MedicalRecordStatus.DRAFT
                                )
                )
                .orElse(false);
    }

    /**
     * Autorização para escrever dados clínicos de um paciente
     *
     * Permite se:
     * - Usuário é PSYCHOANALYST
     * - Usuário tem vínculo ACTIVE com o paciente
     */
    public boolean canWriteClinicalData(
            Authentication authentication,
            Long patientId
    ) {

        Long userId = contextService
                .userId(authentication)
                .orElse(null);

        if (userId == null) {
            log.debug("Acesso negado: userId não encontrado");
            return false;
        }

        if (!contextService.hasRole(authentication, "PSYCHOANALYST")) {
            log.debug("Acesso negado: usuário não tem role PSYCHOANALYST");
            return false;
        }

        Psychoanalyst psychoanalyst = psychoanalystRepository
                .findByUserId(userId)
                .orElse(null);

        if (psychoanalyst == null) {
            log.debug("Acesso negado: usuário não é psicanalista");
            return false;
        }

        // Verificar se tem vínculo ACTIVE com paciente
        return psychoanalystRepository.existsActiveTherapeuticRelationship(
                psychoanalyst.getId(),
                patientId
        );
    }

    /**
     * Autorização para ler dados clínicos de um paciente
     *
     * Permite se:
     * - Usuário é PSYCHOANALYST
     * - Usuário tem vínculo ACTIVE ou SUSPENDED com o paciente
     */
    public boolean canReadPatientClinicalData(
            Authentication authentication,
            Long patientId
    ) {

        Long userId = contextService
                .userId(authentication)
                .orElse(null);

        if (userId == null) {
            log.debug("Acesso negado: userId não encontrado");
            return false;
        }

        if (!contextService.hasRole(authentication, "PSYCHOANALYST")) {
            log.debug("Acesso negado: usuário não tem role PSYCHOANALYST");
            return false;
        }

        Psychoanalyst psychoanalyst = psychoanalystRepository
                .findByUserId(userId)
                .orElse(null);

        if (psychoanalyst == null) {
            log.debug("Acesso negado: usuário não é psicanalista");
            return false;
        }

        // Verificar se tem vínculo ACTIVE ou SUSPENDED com paciente
        return psychoanalystRepository.existsTherapeuticRelationship(
                psychoanalyst.getId(),
                patientId
        );
    }
}
