package com.psicogest.psicogest.security.authorization;

import com.psicogest.psicogest.repository.PatientRepository;
import com.psicogest.psicogest.repository.PsychoanalystRepository;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("identityAuthorization")
public class IdentityAuthorizationService {

    private final AuthenticatedUserContext context;

    private final PatientRepository patientRepository;

    private final PsychoanalystRepository
            psychoanalystRepository;

    public IdentityAuthorizationService(
            AuthenticatedUserContext context,
            PatientRepository patientRepository,
            PsychoanalystRepository psychoanalystRepository
    ) {

        this.context = context;

        this.patientRepository =
                patientRepository;

        this.psychoanalystRepository =
                psychoanalystRepository;
    }

    public boolean isPatientSelf(
            Authentication authentication,
            Long patientId
    ) {

        if (
                !context.hasRole(
                        authentication,
                        "PATIENT"
                )
        ) {

            return false;
        }

        return context
                .userId(authentication)
                .map(
                        userId ->
                                patientRepository
                                        .existsByIdAndUserId(
                                                patientId,
                                                userId
                                        )
                )
                .orElse(false);
    }

    public boolean isPsychoanalystSelf(
            Authentication authentication,
            Long psychoanalystId
    ) {

        if (
                !context.hasRole(
                        authentication,
                        "PSYCHOANALYST"
                )
        ) {

            return false;
        }

        return context
                .userId(authentication)
                .map(
                        userId ->
                                psychoanalystRepository
                                        .existsByIdAndUserId(
                                                psychoanalystId,
                                                userId
                                        )
                )
                .orElse(false);
    }
}