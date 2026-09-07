package com.psicogest.psicogest.security.authorization;

import com.psicogest.psicogest.model.enums.ClinicAccessRole;
import com.psicogest.psicogest.model.enums.ClinicUserMembershipStatus;
import com.psicogest.psicogest.model.enums.TherapeuticRelationshipStatus;

import com.psicogest.psicogest.repository.*;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

@Component("clinicalAuthorization")
public class ClinicalAuthorizationService {

    private static final EnumSet<
            TherapeuticRelationshipStatus
            > READ_RELATIONSHIP_STATUSES =
            EnumSet.of(
                    TherapeuticRelationshipStatus.ACTIVE,
                    TherapeuticRelationshipStatus.SUSPENDED
            );

    private final AuthenticatedUserContext context;

    private final PatientRepository patientRepository;

    private final PsychoanalystRepository
            psychoanalystRepository;

    private final TherapeuticRelationshipRepository
            relationshipRepository;

    private final AppointmentRepository
            appointmentRepository;

    private final ClinicUserMembershipRepository
            clinicUserMembershipRepository;

    public ClinicalAuthorizationService(
            AuthenticatedUserContext context,
            PatientRepository patientRepository,
            PsychoanalystRepository psychoanalystRepository,
            TherapeuticRelationshipRepository relationshipRepository,
            AppointmentRepository appointmentRepository,
            ClinicUserMembershipRepository clinicUserMembershipRepository
    ) {

        this.context = context;
        this.patientRepository = patientRepository;
        this.psychoanalystRepository = psychoanalystRepository;
        this.relationshipRepository = relationshipRepository;
        this.appointmentRepository = appointmentRepository;
        this.clinicUserMembershipRepository =
                clinicUserMembershipRepository;
    }

    public boolean canReadPatientProfile(
        Authentication authentication,
        Long patientId
) {

    Long userId =
            context
                    .userId(authentication)
                    .orElse(null);

    if (userId == null) {
        return false;
    }

    /*
     * Paciente pode visualizar a si próprio.
     */
    if (
            context.hasRole(
                    authentication,
                    "PATIENT"
            )
    ) {

        return patientRepository
                .existsByIdAndUserId(
                        patientId,
                        userId
                );
    }

    /*
     * Psicanalista precisa de vínculo clínico.
     */
    if (
            context.hasRole(
                    authentication,
                    "PSYCHOANALYST"
            )
    ) {

        return psychoanalystRepository
                .findByUserId(userId)
                .map(
                        psychoanalyst ->
                                relationshipRepository
                                        .existsByPatientIdAndPsychoanalystIdAndStatusIn(
                                                patientId,
                                                psychoanalyst.getId(),
                                                READ_RELATIONSHIP_STATUSES
                                        )
                )
                .orElse(false);
    }

    /*
     * CLINIC_ADMIN será tratado através
     * do contexto administrativo da clínica.
     */
    if (
            context.hasRole(
                    authentication,
                    "CLINIC_ADMIN"
            )
    ) {

        return appointmentRepository
                .existsPatientInClinicAdminScope(
                        patientId,
                        userId
                );
    }

    /*
     * SYSTEM_ADMIN não ganha acesso
     * ao paciente clínico automaticamente.
     */
    return false;
}

public boolean canReadClinicalData(
        Authentication authentication,
        Long patientId
) {

    if (
            !context.hasRole(
                    authentication,
                    "PSYCHOANALYST"
            )
    ) {
        return false;
    }

    Long userId =
            context
                    .userId(authentication)
                    .orElse(null);

    if (userId == null) {
        return false;
    }

    return psychoanalystRepository
            .findByUserId(userId)
            .map(
                    psychoanalyst ->
                            relationshipRepository
                                    .existsByPatientIdAndPsychoanalystIdAndStatusIn(
                                            patientId,
                                            psychoanalyst.getId(),
                                            READ_RELATIONSHIP_STATUSES
                                    )
            )
            .orElse(false);
}

public boolean canWriteClinicalData(
        Authentication authentication,
        Long patientId
) {

    if (
            !context.hasRole(
                    authentication,
                    "PSYCHOANALYST"
            )
    ) {

        return false;
    }

    Long userId =
            context
                    .userId(authentication)
                    .orElse(null);

    if (userId == null) {
        return false;
    }

    return psychoanalystRepository
            .findByUserId(userId)
            .map(
                    psychoanalyst ->
                            relationshipRepository
                                    .findByPatientIdAndPsychoanalystIdAndStatus(
                                            patientId,
                                            psychoanalyst.getId(),
                                            TherapeuticRelationshipStatus.ACTIVE
                                    )
                                    .isPresent()
            )
            .orElse(false);
}

public boolean canReadAppointment(
        Authentication authentication,
        Long appointmentId
) {

    Long userId =
            context
                    .userId(authentication)
                    .orElse(null);

    if (userId == null) {
        return false;
    }

    if (
            context.hasRole(
                    authentication,
                    "PATIENT"
            )
    ) {

        return appointmentRepository
                .existsByIdAndPatientUserId(
                        appointmentId,
                        userId
                );
    }

    if (
            context.hasRole(
                    authentication,
                    "PSYCHOANALYST"
            )
    ) {

        return appointmentRepository
                .existsByIdAndPsychoanalystUserId(
                        appointmentId,
                        userId
                );
    }

    if (
            context.hasRole(
                    authentication,
                    "CLINIC_ADMIN"
            )
    ) {

        return appointmentRepository
                .findClinicIdByAppointmentId(
                        appointmentId
                )
                .map(
                        clinicId ->
                                clinicUserMembershipRepository
                                        .existsByClinicIdAndUserIdAndAccessRoleAndStatus(
                                                clinicId,
                                                userId,
                                                ClinicAccessRole.ADMIN,
                                                ClinicUserMembershipStatus.ACTIVE
                                        )
                )
                .orElse(false);
    }

    return false;
}
}