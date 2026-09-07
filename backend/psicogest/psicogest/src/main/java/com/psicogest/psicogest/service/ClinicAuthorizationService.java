package com.psicogest.psicogest.service;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.psicogest.psicogest.model.enums.ClinicAccessRole;
import com.psicogest.psicogest.model.enums.ClinicUserMembershipStatus;
import com.psicogest.psicogest.repository.ClinicUserMembershipRepository;
import com.psicogest.psicogest.security.authorization.AuthenticatedUserContext;

@Component("clinicAuthorization")
public class ClinicAuthorizationService {

    private final AuthenticatedUserContext context;

    private final ClinicUserMembershipRepository
            membershipRepository;

    public ClinicAuthorizationService(
            AuthenticatedUserContext context,
            ClinicUserMembershipRepository membershipRepository
    ) {

        this.context = context;

        this.membershipRepository =
                membershipRepository;
    }

    public boolean canAdminister(
            Authentication authentication,
            Long clinicId
    ) {

        /*
         * SYSTEM_ADMIN pode administrar
         * estrutura da plataforma.
         *
         * Isso NÃO concede acesso clínico.
         */
        if (
                context.hasRole(
                        authentication,
                        "SYSTEM_ADMIN"
                )
        ) {
            return true;
        }

        if (
                !context.hasRole(
                        authentication,
                        "CLINIC_ADMIN"
                )
        ) {
            return false;
        }

        return context
                .userId(authentication)
                .map(
                        userId ->
                                membershipRepository
                                        .existsByClinicIdAndUserIdAndAccessRoleAndStatus(
                                                clinicId,
                                                userId,
                                                ClinicAccessRole.ADMIN,
                                                ClinicUserMembershipStatus.ACTIVE
                                        )
                )
                .orElse(false);
    }
}