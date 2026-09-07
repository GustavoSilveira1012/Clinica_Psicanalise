package com.psicogest.psicogest.security.mfa;
import org.apache.commons.codec.binary.Base32;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.Base64;
import static org.assertj.core.api.Assertions.*;

class TotpServiceTest {
    private final String secret = new Base32().encodeToString("12345678901234567890".getBytes(StandardCharsets.US_ASCII));
    @Test void matchesRfc6238Sha1VectorsAndRejectsReplay() {
        long[] times = {59L, 1111111109L, 1111111111L, 1234567890L, 2000000000L, 20000000000L};
        String[] codes = {"287082", "081804", "050471", "005924", "279037", "353130"};
        for (int i = 0; i < times.length; i++) {
            var totp = new TotpService(Clock.fixed(Instant.ofEpochSecond(times[i]), ZoneOffset.UTC));
            long step = times[i] / 30;
            assertThat(totp.codeAtStep(secret, step)).isEqualTo(codes[i]);
            assertThat(totp.verifyAndReturnStep(secret, codes[i], null)).hasValue(step);
            assertThat(totp.verifyAndReturnStep(secret, codes[i], step)).isEmpty();
        }
    }
    @Test void acceptsOnlyOneStepOfClockDriftAndSixAsciiDigits() {
        var totp = new TotpService(Clock.fixed(Instant.ofEpochSecond(3000), ZoneOffset.UTC));
        assertThat(totp.verifyAndReturnStep(secret, totp.codeAtStep(secret, 99), null)).hasValue(99);
        assertThat(totp.verifyAndReturnStep(secret, totp.codeAtStep(secret, 101), null)).hasValue(101);
        assertThat(totp.verifyAndReturnStep(secret, totp.codeAtStep(secret, 98), null)).isEmpty();
        assertThat(totp.verifyAndReturnStep(secret, "１２３４５６", null)).isEmpty();
        assertThat(totp.verifyAndReturnStep(secret, null, null)).isEmpty();
    }
    @Test void encryptsWithUniqueIvAndRejectsTampering() {
        var cipher = new MfaSecretCipher(properties(new byte[32]));
        var first = cipher.encrypt(secret);
        var second = cipher.encrypt(secret);
        assertThat(first.ciphertext()).isNotEqualTo(secret).isNotEqualTo(second.ciphertext());
        assertThat(first.iv()).isNotEqualTo(second.iv());
        assertThat(cipher.decrypt(first.ciphertext(), first.iv())).isEqualTo(secret);
        byte[] corrupted = Base64.getDecoder().decode(first.ciphertext());
        corrupted[0] ^= 1;
        assertThatThrownBy(() -> cipher.decrypt(Base64.getEncoder().encodeToString(corrupted), first.iv()))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new MfaSecretCipher(properties(new byte[16]))).isInstanceOf(IllegalStateException.class);
    }
    private MfaProperties properties(byte[] key) {
        return new MfaProperties(Duration.ofMinutes(5), 5, "PsicoGest", Base64.getEncoder().encodeToString(key));
    }
}
