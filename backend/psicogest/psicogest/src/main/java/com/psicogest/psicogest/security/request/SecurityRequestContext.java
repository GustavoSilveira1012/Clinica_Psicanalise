package com.psicogest.psicogest.security.request;

public record SecurityRequestContext(
        String sourceIp,
        String userAgentHash,
        String method,
        String path
) {
}
