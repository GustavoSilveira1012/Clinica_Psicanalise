package com.psicogest.psicogest.security;
import com.psicogest.psicogest.exception.RefreshTokenReuseDetectedException;
import com.psicogest.psicogest.model.entity.*;
import com.psicogest.psicogest.model.enums.UserRole;
import com.psicogest.psicogest.repository.*;
import com.psicogest.psicogest.security.jwt.JwtProperties;
import com.psicogest.psicogest.security.refresh.SecurityTokenGenerator;
import com.psicogest.psicogest.service.*;
import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RefreshTokenSecurityTest {
    RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
    UserRepository users = mock(UserRepository.class);
    UserSessionRepository sessions = mock(UserSessionRepository.class);
    UserSessionService sessionService = mock(UserSessionService.class);
    MfaMethodRepository methods = mock(MfaMethodRepository.class);
    SecurityTokenGenerator generator = new SecurityTokenGenerator();
    RefreshTokenService service;
    User user = User.builder().id(15L).role(UserRole.PATIENT).active(true).build();
    UUID sid = UUID.randomUUID();
    @BeforeEach void setUp() {
        var properties = new JwtProperties("issuer", "audience", Duration.ofMinutes(10), Duration.ofDays(14),
                null, null, "test", "rt", false);
        service = new RefreshTokenService(repository, generator, properties, users, sessions, sessionService,
                methods, Clock.systemUTC());
        when(repository.saveAndFlush(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));
    }
    @Test void persistsOnlyHashAndUsesSessionAsFamily() {
        var token = service.issueInitial(user, sid, "127.0.0.1", "agent");
        assertThat(token.entity().getTokenHash()).isEqualTo(generator.hash(token.rawToken())).isNotEqualTo(token.rawToken());
        assertThat(token.entity().getFamilyId()).isEqualTo(sid);
    }
    @Test void rotatesAndTouchesSession() {
        String raw = generator.generate();
        var current = existing(raw);
        var session = UserSession.builder().id(sid).user(user).expiresAt(LocalDateTime.now().plusDays(1)).build();
        when(sessions.findByIdAndUserId(sid, 15L)).thenReturn(Optional.of(session));
        var result = service.rotate(raw, "127.0.0.2", "agent");
        assertThat(current.getConsumedAt()).isNotNull();
        assertThat(current.getReplacedBy()).isNotNull();
        assertThat(result.sessionId()).isEqualTo(sid);
        assertThat(session.getLastIp()).isEqualTo("127.0.0.2");
        assertThat(session.getLastSeenAt()).isNotNull();
    }
    @Test void reuseRevokesTheWholeSession() {
        String raw = generator.generate();
        existing(raw).setConsumedAt(LocalDateTime.now().minusMinutes(1));
        assertThatThrownBy(() -> service.rotate(raw, "ip", "agent")).isInstanceOf(RefreshTokenReuseDetectedException.class);
        verify(sessionService).revoke(15L, sid, "REFRESH_TOKEN_REUSE");
    }
    @Test void logoutRevokesSession() {
        String raw = generator.generate();
        existing(raw);
        service.revokeCurrentSession(raw);
        verify(sessionService).revoke(15L, sid, "LOGOUT");
    }
    private RefreshToken existing(String raw) {
        var current = RefreshToken.builder().id(UUID.randomUUID()).user(user).familyId(sid)
                .tokenHash(generator.hash(raw)).securityVersion(1)
                .issuedAt(LocalDateTime.now()).expiresAt(LocalDateTime.now().plusDays(1)).build();
        when(repository.findUserIdByHash(generator.hash(raw))).thenReturn(Optional.of(15L));
        when(users.findByIdForUpdate(15L)).thenReturn(Optional.of(user));
        when(repository.findByTokenHashForUpdate(generator.hash(raw))).thenReturn(Optional.of(current));
        return current;
    }
}
