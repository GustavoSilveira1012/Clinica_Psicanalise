package com.psicogest.psicogest.security.mfa;

import com.psicogest.psicogest.controller.*;
import com.psicogest.psicogest.dto.auth.LoginRequest;
import com.psicogest.psicogest.exception.*;
import com.psicogest.psicogest.model.entity.*;
import com.psicogest.psicogest.model.enums.*;
import com.psicogest.psicogest.repository.*;
import com.psicogest.psicogest.security.config.SecurityConfig;
import com.psicogest.psicogest.security.handler.*;
import com.psicogest.psicogest.security.jwt.*;
import com.psicogest.psicogest.security.refresh.*;
import com.psicogest.psicogest.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.core.io.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.DriverManager;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(classes = MfaSessionIntegrationTest.TestApplication.class, properties = {
        "spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.show-sql=false", "spring.jpa.open-in-view=false"})
@AutoConfigureMockMvc
class MfaSessionIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) throws Exception {
        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            var scripts = new ResourceDatabasePopulator(new ClassPathResource("auth-test-users.sql"),
                    new ClassPathResource("bd/migration/V12__add_user_security_fields.sql"),
                    new ClassPathResource("bd/migration/V13__create_refresh_tokens.sql"),
                    new ClassPathResource("bd/migration/V14__create_mfa_infrastructure.sql"),
                    new ClassPathResource("bd/migration/V15__create_user_sessions.sql"));
            scripts.populate(connection);
        }
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired AuthService auth;
    @Autowired MfaService mfa;
    @Autowired TotpService totp;
    @Autowired UserSessionService sessions;
    @Autowired RefreshTokenService refresh;
    @Autowired UserRepository users;
    @Autowired UserSessionRepository sessionRepository;
    @Autowired AuthenticationChallengeRepository challenges;
    @Autowired MfaMethodRepository methods;
    @Autowired SecurityTokenGenerator generator;
    @Autowired JwtDecoder decoder;
    @Autowired JwtService jwt;
    @Autowired PasswordEncoder passwords;
    @Autowired MutableClock clock;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired tools.jackson.databind.json.JsonMapper mapper;

    @BeforeEach void reset() {
        jdbc.execute("TRUNCATE users, authentication_challenges, mfa_methods, mfa_recovery_codes, refresh_tokens, user_sessions RESTART IDENTITY CASCADE");
        clock.instant = Instant.now();
    }

    @Test void mandatoryRolesGetEnrollmentWithoutAnyTokensOrSession() {
        for (var role : new UserRole[]{UserRole.PSYCHOANALYST, UserRole.CLINIC_ADMIN, UserRole.SYSTEM_ADMIN}) {
            User user = user(role);
            var login = login(user);
            assertThat(login.response().status()).isEqualTo(LoginStatus.MFA_ENROLLMENT_REQUIRED);
            assertThat(login.response().authentication()).isNull();
            assertThat(login.refreshToken()).isNull();
            assertThat(login.response().challenge()).hasSize(43);
            assertThat(challenges.findAll()).allSatisfy(c ->
                    assertThat(c.getTokenHash()).hasSize(64).isNotEqualTo(login.response().challenge()));
        }
        assertThat(sessionRepository.count()).isZero();
        assertThat(count("refresh_tokens")).isZero();
    }

    @Test void patientEnrollmentNeedsPasswordAndInvalidatesEarlierPasswordOnlySession() {
        User user = user(UserRole.PATIENT);
        var login = login(user);
        assertThat(login.response().status()).isEqualTo(LoginStatus.AUTHENTICATED);
        String access = login.response().authentication().accessToken();
        assertThat(decoder.decode(access).getSubject()).isEqualTo(user.getId().toString());
        assertThatThrownBy(() -> mfa.enrollment(user.getId(), "wrong", "ip", "agent"))
                .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);
        String challenge = mfa.enrollment(user.getId(), "secret", "127.0.0.1", "agent");
        var setup = mfa.setup(challenge);
        mfa.confirm(challenge, code(setup.secret()), "127.0.0.1", "agent");
        assertThatThrownBy(() -> decoder.decode(access)).isInstanceOf(JwtException.class);
        assertThat(login(user).response().status()).isEqualTo(LoginStatus.MFA_REQUIRED);
    }

    @Test void enrollmentEncryptsSecretAndReturnsRecoveryCodesOnlyOnce() {
        User user = user(UserRole.PSYCHOANALYST);
        String challenge = login(user).response().challenge();
        var setup = mfa.setup(challenge);
        assertThat(setup.otpauthUri()).startsWith("otpauth://totp/PsicoGest:").contains("digits=6&period=30");
        var stored = methods.findAll().getFirst();
        assertThat(stored.getSecretCiphertext()).isNotEqualTo(setup.secret());
        assertThat(stored.getSecretIv()).isNotBlank();
        assertThatThrownBy(() -> mfa.setup(challenge)).isInstanceOf(InvalidMfaException.class);
        var result = mfa.confirm(challenge, code(setup.secret()), "127.0.0.1", "agent");
        assertThat(result.recoveryCodes()).hasSize(10).doesNotHaveDuplicates();
        assertThat(count("mfa_recovery_codes")).isEqualTo(10);
        for (String recoveryCode : result.recoveryCodes()) {
            assertThat(jdbc.queryForObject("select count(*) from mfa_recovery_codes where code_hash = ?",
                    Long.class, generator.hash(recoveryCode))).isEqualTo(1);
        }
        assertThat(decoder.decode(result.tokens().response().accessToken()).getClaimAsString("sid")).isNotBlank();
        assertThatThrownBy(() -> mfa.confirm(challenge, code(setup.secret()), "127.0.0.1", "agent"))
                .isInstanceOf(InvalidMfaException.class);
    }

    @Test void wrongCodesCommitAttemptsAndFifthAttemptConsumesChallenge() {
        User user = user(UserRole.PSYCHOANALYST);
        String challenge = login(user).response().challenge();
        var setup = mfa.setup(challenge);
        for (int i = 1; i <= 5; i++) {
            assertThatThrownBy(() -> mfa.confirm(challenge, "invalid", "127.0.0.1", "agent"))
                    .isInstanceOf(InvalidMfaException.class);
            assertThat(challenges.findAll().getFirst().getAttemptCount()).isEqualTo(i);
        }
        assertThat(challenges.findAll().getFirst().getConsumedAt()).isNotNull();
        assertThatThrownBy(() -> mfa.confirm(challenge, code(setup.secret()), "127.0.0.1", "agent"))
                .isInstanceOf(InvalidMfaException.class);
        assertThat(count("refresh_tokens")).isZero();
    }

    @Test void expiredChallengeAndSupersededEnrollmentAreRejected() {
        User user = user(UserRole.PSYCHOANALYST);
        String first = login(user).response().challenge();
        var setup = mfa.setup(first);
        String second = login(user).response().challenge();
        mfa.setup(second);
        assertThatThrownBy(() -> mfa.confirm(first, code(setup.secret()), "127.0.0.1", "agent"))
                .isInstanceOf(InvalidMfaException.class);
        clock.advance(301);
        assertThatThrownBy(() -> mfa.setup(second)).isInstanceOf(InvalidMfaException.class);
    }

    @Test void validOtpIssuesTokensButReplayAcrossChallengesIsRejected() {
        var enrolled = enroll();
        clock.advance(30);
        String challenge = login(enrolled.user()).response().challenge();
        var verified = mfa.verify(challenge, code(enrolled.secret()), "127.0.0.1", "agent");
        decoder.decode(verified.response().accessToken());
        String next = login(enrolled.user()).response().challenge();
        assertThatThrownBy(() -> mfa.verify(next, code(enrolled.secret()), "127.0.0.1", "agent"))
                .isInstanceOf(InvalidMfaException.class);
        assertThatThrownBy(() -> mfa.verify(challenge, code(enrolled.secret()), "127.0.0.1", "agent"))
                .isInstanceOf(InvalidMfaException.class);
    }

    @Test void concurrentOtpRequestsHaveExactlyOneWinner() throws Exception {
        var enrolled = enroll();
        clock.advance(30);
        String first = login(enrolled.user()).response().challenge();
        String second = login(enrolled.user()).response().challenge();
        String code = code(enrolled.secret());
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            List<Future<Boolean>> results = new ArrayList<>();
            for (String challenge : List.of(first, second)) {
                results.add(pool.submit(() -> {
                    start.await();
                    try { mfa.verify(challenge, code, "127.0.0.1", "agent"); return true; }
                    catch (InvalidMfaException e) { return false; }
                }));
            }
            start.countDown();
            int winners = 0;
            for (var result : results) if (result.get(15, TimeUnit.SECONDS)) winners++;
            assertThat(winners).isEqualTo(1);
        }
    }

    @Test void recoveryCodeCanOnlyBeUsedOnce() {
        var enrolled = enroll();
        String recoveryCode = enrolled.result().recoveryCodes().getFirst();
        String first = login(enrolled.user()).response().challenge();
        var result = mfa.recover(first, recoveryCode, "127.0.0.1", "agent");
        decoder.decode(result.response().accessToken());
        String second = login(enrolled.user()).response().challenge();
        assertThatThrownBy(() -> mfa.recover(second, recoveryCode, "127.0.0.1", "agent"))
                .isInstanceOf(InvalidMfaException.class);
        assertThat(jdbc.queryForObject("select count(*) from mfa_recovery_codes where used_at is not null", Long.class)).isEqualTo(1);
    }

    @Test void sessionRotationRevocationAndOwnershipAreEnforced() throws Exception {
        User user = user(UserRole.PATIENT);
        var login = login(user);
        var access = decoder.decode(login.response().authentication().accessToken());
        UUID sid = UUID.fromString(access.getClaimAsString("sid"));
        var otherDevice = login(user);
        var previous = sessionRepository.findById(sid).orElseThrow().getLastSeenAt();
        clock.advance(30);
        var rotated = auth.refresh(login.refreshToken(), "127.0.0.2", "agent");
        var session = sessionRepository.findById(sid).orElseThrow();
        assertThat(session.getLastSeenAt()).isAfter(previous);
        assertThat(session.getLastIp()).isEqualTo("127.0.0.2");
        assertThat(sessions.list(user.getId(), sid)).hasSize(2);
        assertThat(sessions.list(user.getId(), sid).stream().filter(s -> s.current()).count()).isEqualTo(1);
        User other = user(UserRole.PATIENT);
        assertThat(sessions.list(other.getId(), sid)).isEmpty();
        assertThatThrownBy(() -> sessions.revoke(other.getId(), sid, "TEST")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> decoder.decode(jwt.issueAccessToken(other, sid).value())).isInstanceOf(JwtException.class);
        mvc.perform(get("/auth/sessions").header("Authorization", "Bearer " + rotated.response().accessToken()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].current").value(true));
        mvc.perform(delete("/auth/sessions/" + sid).with(csrfRequest())
                .header("Authorization", "Bearer " + rotated.response().accessToken())).andExpect(status().isNoContent());
        assertThatThrownBy(() -> auth.refresh(rotated.refreshToken(), "127.0.0.2", "agent"))
                .isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> decoder.decode(rotated.response().accessToken())).isInstanceOf(JwtException.class);
        decoder.decode(otherDevice.response().authentication().accessToken());
        auth.refresh(otherDevice.refreshToken(), "127.0.0.3", "another device");
        mvc.perform(get("/auth/sessions").header("Authorization", "Bearer " + rotated.response().accessToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test void refreshReuseCommitsRevocationDespiteUnauthorizedResponse() {
        User user = user(UserRole.PATIENT);
        var login = login(user);
        var rotated = auth.refresh(login.refreshToken(), "127.0.0.1", "agent");
        assertThatThrownBy(() -> auth.refresh(login.refreshToken(), "127.0.0.1", "agent"))
                .isInstanceOf(RefreshTokenReuseDetectedException.class);
        assertThatThrownBy(() -> decoder.decode(rotated.response().accessToken())).isInstanceOf(JwtException.class);
        assertThat(sessionRepository.findAll().getFirst().getRevokedAt()).isNotNull();
    }

    @Test void logoutAllInvalidatesSessionsRefreshAndOutstandingChallenges() {
        var enrolled = enroll();
        String recoveryChallenge = login(enrolled.user()).response().challenge();
        var secondDevice = mfa.recover(recoveryChallenge, enrolled.result().recoveryCodes().getFirst(), "127.0.0.2", "agent");
        String challenge = login(enrolled.user()).response().challenge();
        auth.logoutAll(enrolled.user().getId());
        assertThat(sessions.list(enrolled.user().getId(), UUID.randomUUID())).isEmpty();
        assertThatThrownBy(() -> decoder.decode(enrolled.result().tokens().response().accessToken())).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> decoder.decode(secondDevice.response().accessToken())).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> auth.refresh(secondDevice.refreshToken(), "127.0.0.2", "agent"))
                .isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> auth.refresh(enrolled.result().tokens().refreshToken(), "127.0.0.1", "agent"))
                .isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> mfa.recover(challenge, enrolled.result().recoveryCodes().get(1), "127.0.0.1", "agent"))
                .isInstanceOf(InvalidMfaException.class);
    }

    @Test void removalNeedsFreshPasswordChallengeAndCurrentOtpThenRevokesEverything() {
        var enrolled = enroll();
        clock.advance(30);
        assertThatThrownBy(() -> mfa.remove(enrolled.user().getId(), "missing", code(enrolled.secret())))
                .isInstanceOf(InvalidMfaException.class);
        String challenge = login(enrolled.user()).response().challenge();
        mfa.remove(enrolled.user().getId(), challenge, code(enrolled.secret()));
        assertThat(methods.findAll().getFirst().getStatus()).isEqualTo(MfaMethodStatus.REVOKED);
        assertThat(methods.findAll().getFirst().getSecretCiphertext()).isNull();
        assertThatThrownBy(() -> decoder.decode(enrolled.result().tokens().response().accessToken())).isInstanceOf(JwtException.class);
        assertThat(jdbc.queryForObject("select count(*) from mfa_recovery_codes where used_at is null", Long.class)).isZero();
        assertThat(login(enrolled.user()).response().status()).isEqualTo(LoginStatus.MFA_ENROLLMENT_REQUIRED);
    }

    @Test void publicMfaEndpointsRequireCsrfButNotBearerAndNeverAcceptBearerOnlyRemoval() throws Exception {
        mvc.perform(post("/auth/mfa/totp/setup").contentType("application/json").content("{\"challenge\":\"invalid\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/auth/mfa/totp/setup").with(csrfRequest())
                .contentType("application/json").content("{\"challenge\":\"invalid\"}"))
                .andExpect(status().isUnauthorized());
        var login = login(user(UserRole.PATIENT));
        mvc.perform(delete("/auth/mfa/totp").with(csrfRequest())
                .header("Authorization", "Bearer " + login.response().authentication().accessToken())
                .contentType("application/json").content("{}")).andExpect(status().isBadRequest());
    }

    @Test void passwordFailuresArePersisted() {
        User user = user(UserRole.PATIENT);
        assertThatThrownBy(() -> auth.login(new LoginRequest(user.getEmail(), "wrong"), "127.0.0.1", "agent"))
                .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);
        assertThat(users.findById(user.getId()).orElseThrow().getFailedLoginAttempts()).isEqualTo(1);
    }

    @Test void completeHttpEnrollmentUsesCsrfAndIssuesHttpOnlyRefreshOnlyAfterMfa() throws Exception {
        User user = user(UserRole.PSYCHOANALYST);
        var csrfResponse = mvc.perform(get("/auth/csrf")).andExpect(status().isOk()).andReturn().getResponse();
        String csrfToken = mapper.readTree(csrfResponse.getContentAsString()).get("token").asText();
        var csrfCookie = csrfResponse.getCookie("psicogest_csrf");
        var loginResponse = mvc.perform(post("/auth/login").cookie(csrfCookie).header("X-CSRF-TOKEN", csrfToken)
                .contentType("application/json").content(mapper.writeValueAsString(Map.of("email", user.getEmail(), "password", "secret"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("MFA_ENROLLMENT_REQUIRED"))
                .andExpect(jsonPath("$.authentication").isEmpty()).andReturn().getResponse();
        assertThat(loginResponse.getCookie("psicogest_rt").getMaxAge()).isZero();
        String challenge = mapper.readTree(loginResponse.getContentAsString()).get("challenge").asText();
        var setupResponse = mvc.perform(post("/auth/mfa/totp/setup").cookie(csrfCookie).header("X-CSRF-TOKEN", csrfToken)
                .contentType("application/json").content(mapper.writeValueAsString(Map.of("challenge", challenge))))
                .andExpect(status().isOk()).andReturn().getResponse();
        assertThat(setupResponse.getHeader("Cache-Control")).contains("no-store");
        String secret = mapper.readTree(setupResponse.getContentAsString()).get("secret").asText();
        var confirmResponse = mvc.perform(post("/auth/mfa/totp/confirm").cookie(csrfCookie).header("X-CSRF-TOKEN", csrfToken)
                .contentType("application/json").content(mapper.writeValueAsString(Map.of("challenge", challenge, "code", code(secret)))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.authentication.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.recoveryCodes.length()").value(10)).andReturn().getResponse();
        assertThat(confirmResponse.getCookie("psicogest_rt").isHttpOnly()).isTrue();
        assertThat(confirmResponse.getCookie("psicogest_rt").getMaxAge()).isPositive();
        assertThat(confirmResponse.getContentAsString()).doesNotContain(confirmResponse.getCookie("psicogest_rt").getValue());
    }

    @Test void sessionMigrationBackfillsExistingFamiliesWithoutResurrectingRevokedOnes() throws Exception {
        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            String schema = "migration_" + UUID.randomUUID().toString().replace("-", "");
            try (var statement = connection.createStatement()) { statement.execute("CREATE SCHEMA " + schema); }
            connection.setSchema(schema);
            new ResourceDatabasePopulator(new ClassPathResource("auth-test-users.sql"),
                    new ClassPathResource("bd/migration/V12__add_user_security_fields.sql"),
                    new ClassPathResource("bd/migration/V13__create_refresh_tokens.sql"),
                    new ClassPathResource("bd/migration/V14__create_mfa_infrastructure.sql")).populate(connection);
            UUID activeFamily = UUID.randomUUID();
            UUID revokedFamily = UUID.randomUUID();
            try (var statement = connection.createStatement()) {
                statement.execute("INSERT INTO users (name,email,password_hash,role,active,created_at,updated_at)"
                        + " VALUES ('Test','migration@example.test','test-only','PATIENT',true,now(),now())");
            }
            try (var insert = connection.prepareStatement("INSERT INTO refresh_tokens"
                    + " (id,user_id,family_id,token_hash,security_version,issued_at,expires_at,revoked_at)"
                    + " VALUES (?,1,?,?,1,now(),now()+interval '14 days',?)")) {
                for (UUID family : List.of(activeFamily, activeFamily, revokedFamily)) {
                    insert.setObject(1, UUID.randomUUID());
                    insert.setObject(2, family);
                    insert.setString(3, generator.hash(generator.generate()));
                    insert.setObject(4, family.equals(revokedFamily) ? LocalDateTime.now() : null);
                    insert.executeUpdate();
                }
            }
            new ResourceDatabasePopulator(new ClassPathResource("bd/migration/V15__create_user_sessions.sql")).populate(connection);
            try (var statement = connection.createStatement();
                 var result = statement.executeQuery("SELECT id, revoked_at FROM user_sessions")) {
                int rows = 0;
                while (result.next()) {
                    rows++;
                    if (result.getObject(1, UUID.class).equals(activeFamily)) assertThat(result.getTimestamp(2)).isNull();
                    else assertThat(result.getTimestamp(2)).isNotNull();
                }
                assertThat(rows).isEqualTo(2);
            }
        } // The entire PostgreSQL container, including this test-only schema, is disposed by Testcontainers.
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor csrfRequest() throws Exception {
        var response = mvc.perform(get("/auth/csrf")).andExpect(status().isOk()).andReturn().getResponse();
        var cookie = response.getCookie("psicogest_csrf");
        String token = mapper.readTree(response.getContentAsString()).get("token").asText();
        return request -> {
            request.setCookies(cookie);
            request.addHeader("X-CSRF-TOKEN", token);
            return request;
        };
    }

    private User user(UserRole role) {
        return users.saveAndFlush(User.builder().name("Test").email(UUID.randomUUID() + "@example.test")
                .passwordHash(passwords.encode("secret")).role(role).active(true).build());
    }
    private AuthService.LoginResult login(User user) {
        return auth.login(new LoginRequest(user.getEmail(), "secret"), "127.0.0.1", "Chrome/ Windows");
    }
    private String code(String secret) { return totp.codeAtStep(secret, clock.instant().getEpochSecond() / 30); }
    private long count(String table) { return jdbc.queryForObject("select count(*) from " + table, Long.class); }
    private Enrolled enroll() {
        User user = user(UserRole.PSYCHOANALYST);
        String challenge = login(user).response().challenge();
        var setup = mfa.setup(challenge);
        return new Enrolled(user, setup.secret(), mfa.confirm(challenge, code(setup.secret()), "127.0.0.1", "agent"));
    }
    record Enrolled(User user, String secret, MfaService.EnrollmentResult result) {}

    static class MutableClock extends Clock {
        volatile Instant instant = Instant.now();
        void advance(long seconds) { instant = instant.plusSeconds(seconds); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableJpaRepositories(basePackageClasses = UserRepository.class, includeFilters = @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE, classes = {UserRepository.class, RefreshTokenRepository.class,
            AuthenticationChallengeRepository.class, MfaMethodRepository.class, MfaRecoveryCodeRepository.class,
            UserSessionRepository.class}))
    @Import({AuthService.class, AuthTokenService.class, RefreshTokenService.class, MfaService.class,
            UserSessionService.class, ChallengeService.class, TotpService.class, MfaSecretCipher.class,
            SecurityTokenGenerator.class, JwtService.class, JwtConfiguration.class, JwtKeyLoader.class,
            AccountStateJwtValidator.class, SecurityConfig.class, RestAuthenticationEntryPoint.class,
            RestAccessDeniedHandler.class, RefreshCookieService.class, AuthController.class, MfaController.class,
            UserSessionController.class, GlobalExceptionHandler.class})
    static class TestApplication {
        @Bean PersistenceManagedTypes managedTypes() {
            return PersistenceManagedTypes.of(User.class.getName(), RefreshToken.class.getName(),
                    AuthenticationChallenge.class.getName(), MfaMethod.class.getName(),
                    MfaRecoveryCode.class.getName(), UserSession.class.getName());
        }
        @Bean MutableClock clock() { return new MutableClock(); }
        @Bean PasswordEncoder passwords() { return new BCryptPasswordEncoder(4); }
        @Bean MfaProperties mfaProperties() {
            byte[] key = new byte[32];
            new SecureRandom().nextBytes(key);
            return new MfaProperties(Duration.ofMinutes(5), 5, "PsicoGest", Base64.getEncoder().encodeToString(key));
        }
        @Bean @Primary JwtProperties testJwtProperties() throws Exception {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            var pair = generator.generateKeyPair();
            return new JwtProperties("psicogest-api", "psicogest-web", Duration.ofMinutes(10), Duration.ofDays(14),
                    pem("PUBLIC", pair.getPublic().getEncoded()), pem("PRIVATE", pair.getPrivate().getEncoded()),
                    "test", "psicogest_rt", false);
        }
        private Resource pem(String type, byte[] bytes) {
            return new ByteArrayResource(("-----BEGIN " + type + " KEY-----\n"
                    + Base64.getMimeEncoder(64, new byte[]{10}).encodeToString(bytes)
                    + "\n-----END " + type + " KEY-----\n").getBytes(StandardCharsets.US_ASCII));
        }
    }
}
