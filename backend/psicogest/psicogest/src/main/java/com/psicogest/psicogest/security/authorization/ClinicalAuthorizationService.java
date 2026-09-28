package com.psicogest.psicogest.security.authorization;

import com.psicogest.psicogest.model.entity.Psychoanalyst;
import com.psicogest.psicogest.model.enums.MedicalRecordStatus;
import com.psicogest.psicogest.repository.AddendumRepository;
import com.psicogest.psicogest.repository.MedicalRecordRepository;
import com.psicogest.psicogest.repository.MedicalRecordRevisionRepository;
import com.psicogest.psicogest.repository.PsychoanalystRepository;
import com.psicogest.psicogest.repository.PatientRepository;
import com.psicogest.psicogest.repository.AppointmentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class ClinicalAuthorizationService {

    private final MedicalRecordRepository medicalRecordRepository;

    private final PsychoanalystRepository psychoanalystRepository;

    private final AddendumRepository addendumRepository;

    private final MedicalRecordRevisionRepository revisionRepository;

    private final PatientRepository patientRepository;

    private final AppointmentRepository appointmentRepository;

    private final SecurityContextService contextService;

    public ClinicalAuthorizationService(
            MedicalRecordRepository medicalRecordRepository,
            PsychoanalystRepository psychoanalystRepository,
            AddendumRepository addendumRepository,
            MedicalRecordRevisionRepository revisionRepository,
            SecurityContextService contextService,
            PatientRepository patientRepository,
            AppointmentRepository appointmentRepository
    ) {

        this.medicalRecordRepository = medicalRecordRepository;
        this.psychoanalystRepository = psychoanalystRepository;
        this.addendumRepository = addendumRepository;
        this.revisionRepository = revisionRepository;
        this.contextService = contextService;
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
    }

    public boolean canReadPatientProfile(Authentication authentication, Long patientId) {
        Long userId = contextService.userId(authentication).orElse(null);
        if (userId == null || patientId == null) return false;

        if (contextService.hasRole(authentication, "PATIENT")) {
            return patientRepository.existsByIdAndUserId(patientId, userId);
        }
        if (contextService.hasRole(authentication, "CLINIC_ADMIN")) {
            return patientRepository.existsById(patientId);
        }
        if (!contextService.hasRole(authentication, "PSYCHOANALYST")) return false;
        return psychoanalystRepository.findByUserId(userId)
                .map(psychoanalyst -> psychoanalystRepository.existsTherapeuticRelationship(
                        psychoanalyst.getId(), patientId))
                .orElse(false);
    }

    public boolean canWritePatientProfile(Authentication authentication, Long patientId) {
        Long userId = contextService.userId(authentication).orElse(null);
        if (userId == null || patientId == null) return false;
        if (contextService.hasRole(authentication, "PATIENT")) {
            return patientRepository.existsByIdAndUserId(patientId, userId);
        }
        if (contextService.hasRole(authentication, "CLINIC_ADMIN")) {
            return patientRepository.existsById(patientId);
        }
        if (!contextService.hasRole(authentication, "PSYCHOANALYST")) return false;
        return psychoanalystRepository.findByUserId(userId)
                .map(psychoanalyst -> psychoanalystRepository.existsActiveTherapeuticRelationship(
                        psychoanalyst.getId(), patientId))
                .orElse(false);
    }

    public boolean canReadAppointment(Authentication authentication, Long appointmentId) {
        Long userId = contextService.userId(authentication).orElse(null);
        if (userId == null || appointmentId == null) return false;
        if (contextService.hasRole(authentication, "PSYCHOANALYST")) {
            return appointmentRepository.existsByIdAndPsychoanalystUserId(appointmentId, userId);
        }
        if (contextService.hasRole(authentication, "PATIENT")) {
            return appointmentRepository.existsByIdAndPatientUserId(appointmentId, userId);
        }
        if (contextService.hasRole(authentication, "CLINIC_ADMIN")) {
            return appointmentRepository.existsByIdAndClinicAdminUserId(appointmentId, userId);
        }
        return false;
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

    /**
     * 11. Autorização para adicionar addendum
     *
     * Permite apenas se:
     * - Usuário é o autor original do prontuário
     * - Prontuário está FINALIZED
     */
    public boolean canAddAddendum(
            Authentication authentication,
            UUID medicalRecordId
    ) {

        if (!contextService.hasRole(authentication, "PSYCHOANALYST")) {
            log.debug("Acesso negado: usuário não tem role PSYCHOANALYST");
            return false;
        }

        Long userId = contextService
                .userId(authentication)
                .orElse(null);

        if (userId == null) {
            log.debug("Acesso negado: userId não encontrado");
            return false;
        }

        return psychoanalystRepository
                .findByUserId(userId)
                .map(psychoanalyst ->
                        medicalRecordRepository
                                .existsByIdAndAuthorPsychoanalystIdAndStatus(
                                        medicalRecordId,
                                        psychoanalyst.getId(),
                                        MedicalRecordStatus.FINALIZED
                                )
                )
                .orElse(false);
    }

    /**
     * 30. Autorização para ler addendum
     *
     * Permite se:
     * - Usuário é PSYCHOANALYST
     * - Usuário pode ler o prontuário ao qual o addendum pertence
     *   (é autor original OU tem vínculo ativo/suspenso com paciente)
     */
    public boolean canReadMedicalRecordAddendum(
            Authentication authentication,
            UUID addendumId
    ) {

        if (!contextService.hasRole(authentication, "PSYCHOANALYST")) {
            log.debug("Acesso negado: usuário não tem role PSYCHOANALYST");
            return false;
        }

        Long userId = contextService
                .userId(authentication)
                .orElse(null);

        if (userId == null) {
            log.debug("Acesso negado: userId não encontrado");
            return false;
        }

        Psychoanalyst psychoanalyst = psychoanalystRepository
                .findByUserId(userId)
                .orElse(null);

        if (psychoanalyst == null) {
            log.debug("Acesso negado: usuário não é psicanalista");
            return false;
        }

        // Verificar se o addendum existe e se o psicanalista pode ler seu prontuário
        return addendumRepository
                .findById(addendumId)
                .map(addendum -> medicalRecordRepository
                        .existsReadableBy(
                                addendum.getMedicalRecord().getId(),
                                psychoanalyst.getId()
                        )
                )
                .orElse(false);
    }

    /**
     * 19. Autorização para ler revisão histórica
     *
     * Permitido APENAS se:
     * - Usuário é PSYCHOANALYST
     * - Usuário é o AUTOR ORIGINAL da revisão
     *
     * Racional (tarefa 20):
     * - Revisões contêm hipóteses descartadas, erros, texto incompleto
     * - Nunca fizeram parte do documento final
     * - Outro profissional não deve receber histórico autoral
     * - Distinção: ver que existem revisões ≠ ler conteúdo histórico
     */
    public boolean canReadMedicalRecordRevision(
            Authentication authentication,
            UUID revisionId
    ) {

        if (!contextService.hasRole(authentication, "PSYCHOANALYST")) {
            log.debug("Acesso negado: usuário não tem role PSYCHOANALYST");
            return false;
        }

        Long userId = contextService
                .userId(authentication)
                .orElse(null);

        if (userId == null) {
            log.debug("Acesso negado: userId não encontrado");
            return false;
        }

        return psychoanalystRepository
                .findByUserId(userId)
                .map(psychoanalyst ->
                        revisionRepository.existsByIdAndAuthorPsychoanalystId(
                                revisionId,
                                psychoanalyst.getId()
                        )
                )
                .orElse(false);
    }

    /**
     * 10. Autorização para ler timeline clínica
     *
     * Permite se:
     * - Usuário é PSYCHOANALYST
     * - Usuário pode ler dados clínicos do paciente
     *   (tem vínculo ACTIVE ou SUSPENDED com o paciente)
     */
    public boolean canReadClinicalTimeline(
            Authentication authentication,
            Long patientId
    ) {

        return canReadPatientClinicalData(
                authentication,
                patientId
        );
    }

    /**
     * 22. Autorização para solicitar export clínico
     *
     * Permite se:
     * - Usuário é PSYCHOANALYST
     * - Usuário pode ler dados clínicos do paciente
     *
     * Rate limit adicional será aplicado no service.
     */
    public boolean canRequestClinicalExport(
            Authentication authentication,
            Long patientId
    ) {

        return canReadPatientClinicalData(
                authentication,
                patientId
        );
    }
}
