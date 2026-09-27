package com.psicogest.psicogest.infrastructure.redis;

import com.psicogest.psicogest.config.AuthActionMailProperties;
import com.psicogest.psicogest.model.enums.AuthActionTokenType;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;

/** Rate limits account-action requests without placing raw IP addresses or email addresses in Redis keys. */
@Component
public class AuthActionRequestRateLimiter {

    private final RedisSecurityRateLimiter redis;
    private final AuthActionMailProperties properties;
    private final byte[] hmacKey;

    public AuthActionRequestRateLimiter(
            RedisSecurityRateLimiter redis,
            AuthActionMailProperties properties
    ) {
        this.redis = redis;
        this.properties = properties;
        this.hmacKey = properties.rateLimitKey() == null || properties.rateLimitKey().isBlank()
                ? new byte[0]
                : Base64.getDecoder().decode(properties.rateLimitKey());
    }

    public boolean allow(String remoteAddress, String normalizedEmail, AuthActionTokenType type) {
        if (remoteAddress == null || remoteAddress.isBlank() || normalizedEmail == null || type == null) {
            return false;
        }
        String ipKey = "auth-action:ip:" + hmac("ip:" + remoteAddress);
        if (!redis.tryConsumeTokenBucket(
                ipKey, properties.requestsPerIp(), properties.rateLimitWindow())) {
            return false;
        }

        String accountKey = "auth-action:address:" + hmac(type.name() + ":" + normalizedEmail);
        return redis.tryConsumeTokenBucket(
                accountKey, properties.requestsPerAddress(), properties.rateLimitWindow());
    }

    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível proteger a chave de rate limit de ações de conta", exception);
        }
    }
}
