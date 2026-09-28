package com.psicogest.psicogest.service;

import com.psicogest.psicogest.model.entity.AuthenticationChallenge;
import com.psicogest.psicogest.model.entity.MfaMethod;
import com.psicogest.psicogest.model.entity.MfaRecoveryCode;
import com.psicogest.psicogest.model.entity.User;
import com.psicogest.psicogest.model.enums.AuthenticationChallengeType;
import com.psicogest.psicogest.model.enums.MfaMethodStatus;
import com.psicogest.psicogest.repository.MfaMethodRepository;
import com.psicogest.psicogest.repository.MfaRecoveryCodeRepository;
import com.psicogest.psicogest.repository.UserRepository;
import com.psicogest.psicogest.security.auth.mfa.ChallengeService;
import com.psicogest.psicogest.security.auth.mfa.MfaSecretCipher;
import com.psicogest.psicogest.security.auth.mfa.TotpService;
import com.psicogest.psicogest.security.auth.refresh.SecurityTokenGenerator;
import com.psicogest.psicogest.security.request.SecurityRequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MfaSuccessfulLoginAuditTest {

    @Mock private ChallengeService challenges;
    @Mock private MfaMethodRepository methods;
    @Mock private MfaRecoveryCodeRepository recovery;
    @Mock private UserRepository users;
    @Mock private TotpService totp;
    @Mock private MfaSecretCipher cipher;
    @Mock private SecurityTokenGenerator generator;
    @Mock private AuthTokenService tokens;
    @Mock private UserSessionService sessions;
    @Mock private PasswordEncoder passwords;
    @Mock private BruteForceProtectionService bruteForceProtection;

    private MfaService service;
    private AuthenticationChallenge challenge;
    private User user;
    private SecurityRequestContext context;

    @BeforeEach
    void setUp() {
        service = new MfaService(challenges, methods, recovery, users, totp, cipher, null,
                generator, tokens, sessions, passwords, Clock.systemUTC(), bruteForceProtection);
        user = User.builder().id(71L).build();
        challenge = AuthenticationChallenge.builder()
                .id(UUID.randomUUID())
                .user(user)
                .challengeType(AuthenticationChallengeType.MFA_REQUIRED)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .createdAt(LocalDateTime.now())
                .build();
        context = new SecurityRequestContext("192.0.2.10", "user-agent-hash", "POST", "/auth/mfa/totp/verify");
    }

    @Test
    void recordsSuccessfulLoginAfterValidTotp() {
        MfaMethod method = MfaMethod.builder().secretCiphertext("encrypted").secretIv("iv").build();
        when(challenges.require("challenge", AuthenticationChallengeType.MFA_REQUIRED)).thenReturn(challenge);
        when(methods.findByUserIdAndStatus(71L, MfaMethodStatus.ACTIVE)).thenReturn(Optional.of(method));
        when(cipher.decrypt("encrypted", "iv")).thenReturn("secret");
        when(totp.verifyAndReturnStep("secret", "123456", null)).thenReturn(OptionalLong.of(42));

        service.verify("challenge", "123456", context);

        verify(bruteForceProtection).registerSuccess(user, context);
    }

    @Test
    void recordsSuccessfulLoginAfterValidRecoveryCode() {
        MfaRecoveryCode recoveryCode = MfaRecoveryCode.builder().codeHash("hashed").build();
        when(challenges.require("challenge", AuthenticationChallengeType.MFA_REQUIRED)).thenReturn(challenge);
        when(methods.findByUserIdAndStatus(71L, MfaMethodStatus.ACTIVE))
                .thenReturn(Optional.of(MfaMethod.builder().build()));
        when(generator.hash("recovery-code")).thenReturn("hashed");
        when(recovery.findByUserIdAndCodeHashAndUsedAtIsNull(71L, "hashed"))
                .thenReturn(Optional.of(recoveryCode));

        service.recover("challenge", "recovery-code", context);

        verify(bruteForceProtection).registerSuccess(user, context);
    }
}
