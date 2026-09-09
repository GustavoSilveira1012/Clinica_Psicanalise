package com.psicogest.psicogest.service;

import com.psicogest.psicogest.model.entity.User;
import com.psicogest.psicogest.repository.RefreshTokenRepository;
import com.psicogest.psicogest.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class SecurityResponseService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public SecurityResponseService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public void containAccount(
            Long userId,
            Duration duration,
            String reason
    ) {
        User user = userRepository
                .findByIdForSecurityUpdate(userId)
                .orElseThrow();

        LocalDateTime now = LocalDateTime.now();

        user.setLockedUntil(now.plus(duration));
        user.setSecurityVersion(user.getSecurityVersion() + 1);

        userRepository.saveAndFlush(user);

        refreshTokenRepository.revokeAllForUser(
                userId,
                now,
                reason
        );
    }
}
