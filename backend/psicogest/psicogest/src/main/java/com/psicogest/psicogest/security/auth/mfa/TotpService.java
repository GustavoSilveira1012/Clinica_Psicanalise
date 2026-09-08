package com.psicogest.psicogest.security.auth.mfa;

import org.apache.commons.codec.binary.Base32;
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Clock;
import java.util.*;

@Service
public class TotpService {
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    public TotpService(Clock clock) { this.clock = clock; }

    public String generateSecret() {
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        return new Base32().encodeToString(bytes).replace("=", "");
    }

    public OptionalLong verifyAndReturnStep(String secret, String code, Long lastAcceptedStep) {
        if (code == null || !code.matches("[0-9]{6}")) return OptionalLong.empty();
        long current = Math.floorDiv(clock.instant().getEpochSecond(), 30);
        OptionalLong match = OptionalLong.empty();
        for (long step = current - 1; step <= current + 1; step++) {
            boolean equal = MessageDigest.isEqual(codeAtStep(secret, step).getBytes(StandardCharsets.US_ASCII),
                    code.getBytes(StandardCharsets.US_ASCII));
            if (equal && step >= 0 && (lastAcceptedStep == null || step > lastAcceptedStep)) {
                match = OptionalLong.of(step);
            }
        }
        return match;
    }

    // Package-visible for RFC 6238 vector tests.
    String codeAtStep(String secret, long step) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(new Base32().decode(secret), "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(step).array());
            int offset = hash[hash.length - 1] & 15;
            int binary = ByteBuffer.wrap(hash, offset, 4).getInt() & 0x7fffffff;
            return String.format(Locale.ROOT, "%06d", binary % 1_000_000);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível verificar TOTP", e);
        }
    }
}
