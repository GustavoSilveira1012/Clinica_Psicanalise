package com.psicogest.psicogest.service;

import com.psicogest.psicogest.dto.auth.LoginRequest;
import com.psicogest.psicogest.integration.PostgresIntegrationTest;
import com.psicogest.psicogest.model.entity.*;
import com.psicogest.psicogest.model.enums.*;
import com.psicogest.psicogest.repository.*;
import com.psicogest.psicogest.security.auth.mfa.*;
import com.psicogest.psicogest.security.auth.refresh.SecurityTokenGenerator;
import com.psicogest.psicogest.security.request.SecurityRequestContext;
import org.apache.commons.codec.binary.Base32;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

// A finite PostgreSQL lock timeout makes the former REQUIRES_NEW deadlock fail
// promptly instead of hanging the build. Authentication/persistence stay real.
@TestPropertySource(properties = "spring.datasource.hikari.connection-init-sql=SET lock_timeout TO '2s'")
@Timeout(30)
class AuthenticationTransactionIntegrationTest extends PostgresIntegrationTest {
    @Autowired AuthService auth;
    @Autowired MfaService mfa;
    @Autowired ChallengeService challenges;
    @Autowired UserRepository users;
    @Autowired MfaMethodRepository methods;
    @Autowired MfaRecoveryCodeRepository recovery;
    @Autowired MfaSecretCipher cipher;
    @Autowired PasswordEncoder passwords;
    @Autowired SecurityTokenGenerator generator;
    @Autowired JdbcTemplate jdbc;
    @Autowired Clock clock;
    @MockitoBean LoginRateLimitService rateLimit; // Redis bucket is covered by its integration suite.
    private User user;
    private final SecurityRequestContext context = new SecurityRequestContext("192.0.2.21", "synthetic-agent-hash", "POST", "/auth/login");

    @BeforeEach
    void seed() {
        user = users.saveAndFlush(User.builder().name("Synthetic authentication regression")
                .email("auth-lock-" + UUID.randomUUID() + "@example.invalid")
                .passwordHash(passwords.encode("Synthetic-E2E-2026!"))
                .role(UserRole.PATIENT).active(true).failedLoginAttempts(3).securityVersion(1).build());
    }

    @Test
    void rejectedPasswordCommitsCounterAndSecurityEventWithoutSelfBlocking() {
        assertThrows(BadCredentialsException.class, () -> auth.login(new LoginRequest(user.getEmail(), "incorrect-synthetic-password"), context));
        assertEquals(4, users.findById(user.getId()).orElseThrow().getFailedLoginAttempts());
        assertEvent("LOGIN_FAILURE");
    }

    @Test
    void passwordOnlyLoginCommitsSuccessAndSessionWithoutSelfBlocking() {
        var result = auth.login(new LoginRequest(user.getEmail(), "Synthetic-E2E-2026!"), context);
        assertEquals(LoginStatus.AUTHENTICATED, result.response().status());
        assertNotNull(result.refreshToken());
        assertSuccess();
    }

    @Test
    void activeTotpLoginCommitsSuccessAndConsumptionWithoutSelfBlocking() throws Exception {
        String secret = "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP";
        activate(secret);
        String challenge = challenges.issue(user, AuthenticationChallengeType.MFA_REQUIRED, context.sourceIp(), context.userAgentHash());
        Mac hmac = Mac.getInstance("HmacSHA1");
        hmac.init(new SecretKeySpec(new Base32().decode(secret), "HmacSHA1"));
        byte[] hash = hmac.doFinal(ByteBuffer.allocate(8).putLong(clock.instant().getEpochSecond()/30).array());
        int offset = hash[hash.length-1]&15;
        String code = String.format(Locale.ROOT, "%06d", (ByteBuffer.wrap(hash,offset,4).getInt()&0x7fffffff)%1_000_000);
        assertNotNull(mfa.verify(challenge, code, context).response().accessToken());
        assertSuccess();
        assertNotNull(methods.findByUserIdAndStatus(user.getId(), MfaMethodStatus.ACTIVE).orElseThrow().getLastAcceptedTimeStep());
    }

    @Test
    void recoveryLoginCommitsSuccessAndConsumesOneCodeWithoutSelfBlocking() {
        activate("JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP");
        String raw = generator.generate();
        var code = recovery.saveAndFlush(MfaRecoveryCode.builder().id(UUID.randomUUID()).user(user)
                .codeHash(generator.hash(raw)).createdAt(LocalDateTime.now(clock)).build());
        String challenge = challenges.issue(user, AuthenticationChallengeType.MFA_REQUIRED, context.sourceIp(), context.userAgentHash());
        assertNotNull(mfa.recover(challenge, raw, context).response().accessToken());
        assertSuccess();
        assertNotNull(recovery.findById(code.getId()).orElseThrow().getUsedAt());
    }

    private void activate(String secret) {
        var encrypted = cipher.encrypt(secret);
        methods.saveAndFlush(MfaMethod.builder().id(UUID.randomUUID()).user(user).methodType(MfaMethodType.TOTP)
                .status(MfaMethodStatus.ACTIVE).label("Synthetic TOTP").createdAt(LocalDateTime.now(clock))
                .verifiedAt(LocalDateTime.now(clock)).secretCiphertext(encrypted.ciphertext()).secretIv(encrypted.iv()).build());
    }

    private void assertSuccess() {
        var persisted = users.findById(user.getId()).orElseThrow();
        assertEquals(0, persisted.getFailedLoginAttempts());
        assertNotNull(persisted.getLastLoginAt());
        assertEvent("LOGIN_SUCCESS");
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM user_sessions WHERE user_id=?", Integer.class, user.getId()));
    }

    private void assertEvent(String type) {
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM security_events WHERE user_id=? AND event_type=?", Integer.class, user.getId(), type));
    }
}
