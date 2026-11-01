package com.psicogest.psicogest.service;

import com.psicogest.psicogest.model.entity.AuthenticationChallenge;
import com.psicogest.psicogest.model.entity.MfaMethod;
import com.psicogest.psicogest.model.entity.User;
import com.psicogest.psicogest.model.enums.AuthenticationChallengeType;
import com.psicogest.psicogest.model.enums.MfaMethodStatus;
import com.psicogest.psicogest.repository.MfaMethodRepository;
import com.psicogest.psicogest.repository.MfaRecoveryCodeRepository;
import com.psicogest.psicogest.repository.UserRepository;
import com.psicogest.psicogest.security.auth.mfa.ChallengeService;
import com.psicogest.psicogest.security.auth.mfa.MfaProperties;
import com.psicogest.psicogest.security.auth.mfa.MfaSecretCipher;
import com.psicogest.psicogest.security.auth.mfa.TotpService;
import com.psicogest.psicogest.security.auth.refresh.SecurityTokenGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MfaEnrollmentSetupIdempotencyTest {
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

    @BeforeEach
    void setUp() {
        service = new MfaService(challenges, methods, recovery, users, totp, cipher,
                new MfaProperties(Duration.ofMinutes(5), 5, "PsicoGest", "c3ludGhldGljLWtleS1ub3QtMzItYnl0ZXM="),
                generator, tokens, sessions, passwords, Clock.systemUTC(), bruteForceProtection);
    }

    @Test
    void returnsTheExistingSecretForRepeatedSetupOfTheSameEnrollmentChallenge() {
        UUID challengeId = UUID.randomUUID();
        User user = User.builder().id(42L).email("patient@example.test").build();
        AuthenticationChallenge challenge = AuthenticationChallenge.builder().id(challengeId).user(user).build();
        MfaMethod pending = MfaMethod.builder()
                .user(user)
                .status(MfaMethodStatus.PENDING)
                .enrollmentChallengeId(challengeId)
                .secretCiphertext("encrypted-existing-secret")
                .secretIv("existing-iv")
                .build();
        when(challenges.require("enrollment-challenge", AuthenticationChallengeType.MFA_ENROLLMENT_REQUIRED))
                .thenReturn(challenge);
        when(methods.existsByUserIdAndStatus(42L, MfaMethodStatus.ACTIVE)).thenReturn(false);
        when(methods.findByUserIdAndStatus(42L, MfaMethodStatus.PENDING)).thenReturn(Optional.of(pending));
        when(cipher.decrypt("encrypted-existing-secret", "existing-iv")).thenReturn("SYNTHETICOTPSECRET");

        MfaService.SetupResult result = service.setup("enrollment-challenge");

        assertThat(result.secret()).isEqualTo("SYNTHETICOTPSECRET");
        assertThat(result.otpauthUri()).contains("secret=SYNTHETICOTPSECRET", "issuer=PsicoGest");
        verify(methods, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
        verify(totp, never()).generateSecret();
    }
}
