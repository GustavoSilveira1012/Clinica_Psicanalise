package com.psicogest.psicogest.security;

import java.util.UUID;

public record SecurityActor(
        Long userId,
        UUID sessionId,
        String correlationId,
        String sourceIp,
        String userAgentHash
) {
}
