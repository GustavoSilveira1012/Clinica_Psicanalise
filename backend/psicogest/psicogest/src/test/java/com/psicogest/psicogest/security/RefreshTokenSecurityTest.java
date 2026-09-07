package com.psicogest.psicogest.security;

import com.psicogest.psicogest.exception.RefreshTokenReuseDetectedException;
import com.psicogest.psicogest.model.entity.RefreshToken;
import com.psicogest.psicogest.model.entity.User;
import com.psicogest.psicogest.model.enums.UserRole;
import com.psicogest.psicogest.repository.RefreshTokenRepository;
import com.psicogest.psicogest.security.jwt.JwtProperties;
import com.psicogest.psicogest.security.refresh.RefreshTokenGenerator;
import com.psicogest.psicogest.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenSecurityTest {

    @Mock
    private RefreshTokenRepository repository;

    private RefreshTokenGenerator generator;
    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        generator = new RefreshTokenGenerator();
        JwtProperties properties = new JwtProperties(
                "psicogest-api",
                "psicogest-web",
                Duration.ofMinutes(10),
                Duration.ofDays(14),
                null,
                null,
                "test",
                "psicogest_rt",
                false);
        service = new RefreshTokenService(repository, generator, properties);
    }

    @Test
    void shouldPersistOnlyTheHashOfInitialRefreshToken() {
        User user = user();

        RefreshTokenService.IssuedRefreshToken issued =
                service.issueInitial(user, "127.0.0.1", "agent");

        assertThat(issued.rawToken()).isNotBlank();
        assertThat(issued.entity().getTokenHash())
                .isEqualTo(generator.hash(issued.rawToken()))
                .isNotEqualTo(issued.rawToken());
    }

    @Test
    void shouldRotateTokenAndConsumeThePreviousToken() {
        User user = user();
        UUID familyId = UUID.randomUUID();
        String raw = generator.generate();
        RefreshToken current = token(user, familyId, raw);

        when(repository.findByTokenHashForUpdate(generator.hash(raw)))
                .thenReturn(Optional.of(current));

        RefreshTokenService.RotationResult result =
                service.rotate(raw, "127.0.0.1", "agent");

        assertThat(result.user()).isSameAs(user);
        assertThat(result.refreshToken()).isNotEqualTo(raw);
        assertThat(current.getConsumedAt()).isNotNull();
        assertThat(current.getReplacedBy()).isNotNull();
        assertThat(current.getReplacedBy().getFamilyId()).isEqualTo(familyId);
    }

    @Test
    void shouldRevokeFamilyWhenAConsumedTokenIsReused() {
        User user = user();
        UUID familyId = UUID.randomUUID();
        String raw = generator.generate();
        RefreshToken consumed = token(user, familyId, raw);
        consumed.setConsumedAt(LocalDateTime.now().minusMinutes(1));

        when(repository.findByTokenHashForUpdate(generator.hash(raw)))
                .thenReturn(Optional.of(consumed));

        assertThatThrownBy(() -> service.rotate(raw, "127.0.0.1", "agent"))
                .isInstanceOf(RefreshTokenReuseDetectedException.class);

        verify(repository).revokeFamily(
                eq(familyId),
                any(LocalDateTime.class),
                eq("REFRESH_TOKEN_REUSE"));
    }

    @Test
    void shouldRevokeTheCurrentSessionByFamily() {
        User user = user();
        UUID familyId = UUID.randomUUID();
        String raw = generator.generate();
        RefreshToken current = token(user, familyId, raw);

        when(repository.findByTokenHashForUpdate(generator.hash(raw)))
                .thenReturn(Optional.of(current));

        service.revokeCurrentSession(raw);

        verify(repository).revokeFamily(
                eq(familyId),
                any(LocalDateTime.class),
                eq("LOGOUT"));
    }

    private User user() {
        User user = new User();
        user.setId(15L);
        user.setRole(UserRole.PSYCHOANALYST);
        user.setActive(true);
        user.setSecurityVersion(1);
        return user;
    }

    private RefreshToken token(User user, UUID familyId, String raw) {
        LocalDateTime now = LocalDateTime.now();
        return RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .familyId(familyId)
                .tokenHash(generator.hash(raw))
                .securityVersion(1)
                .issuedAt(now)
                .expiresAt(now.plusDays(1))
                .build();
    }
}
