package com.psicogest.psicogest.security.mfa;

import com.psicogest.psicogest.model.entity.User;
import com.psicogest.psicogest.model.enums.UserRole;
import org.springframework.stereotype.Service;

import java.util.EnumSet;

@Service
public class MfaPolicyService {

    private static final EnumSet<UserRole>
            MFA_REQUIRED_ROLES =
            EnumSet.of(
                    UserRole.PSYCHOANALYST,
                    UserRole.CLINIC_ADMIN,
                    UserRole.SYSTEM_ADMIN
            );

    public static boolean requiresMfa(
            User user
    ) {

        return MFA_REQUIRED_ROLES
                .contains(
                        user.getRole()
                );
    }
}