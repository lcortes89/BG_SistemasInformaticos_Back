package com.bgsistemas.api.twofactor;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

// BGSI-276: the codes are the same as the ones of Google/Microsoft Authenticator (RFC 6238 test values)
class TotpGeneratorTest {

    private final TotpGenerator totp = new TotpGenerator();
    // Secret of the RFC: the ASCII text "12345678901234567890"
    private final String rfcSecret = Base32.encode("12345678901234567890".getBytes(StandardCharsets.US_ASCII));

    @Test
    void base32WorksBothWays() {
        assertThat(rfcSecret).isEqualTo("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ");
        assertThat(new String(Base32.decode(rfcSecret), StandardCharsets.US_ASCII)).isEqualTo("12345678901234567890");
    }

    @Test
    void givesTheCodesOfTheRfc() {
        // The RFC shows 8 digits; the apps use the last 6
        assertThat(totp.codeAt(rfcSecret, totp.stepAt(Instant.ofEpochSecond(59)))).isEqualTo("287082");
        assertThat(totp.codeAt(rfcSecret, totp.stepAt(Instant.ofEpochSecond(1111111109)))).isEqualTo("081804");
        assertThat(totp.codeAt(rfcSecret, totp.stepAt(Instant.ofEpochSecond(1234567890)))).isEqualTo("005924");
    }

    @Test
    void acceptsTheCodeOfNowAndOneStepBeforeOrAfter() {
        Instant now = Instant.ofEpochSecond(1234567890);
        long step = totp.stepAt(now);

        assertThat(totp.matchingStep(rfcSecret, totp.codeAt(rfcSecret, step), now)).hasValue(step);
        assertThat(totp.matchingStep(rfcSecret, totp.codeAt(rfcSecret, step - 1), now)).hasValue(step - 1);
        assertThat(totp.matchingStep(rfcSecret, totp.codeAt(rfcSecret, step + 1), now)).hasValue(step + 1);
        assertThat(totp.matchingStep(rfcSecret, totp.codeAt(rfcSecret, step - 2), now)).isEmpty();
    }

    @Test
    void rejectsCodesThatAreNotSixDigits() {
        Instant now = Instant.ofEpochSecond(1234567890);

        assertThat(totp.matchingStep(rfcSecret, null, now)).isEmpty();
        assertThat(totp.matchingStep(rfcSecret, "12345", now)).isEmpty();
        assertThat(totp.matchingStep(rfcSecret, "abcdef", now)).isEmpty();
    }

    @Test
    void newSecretsAreRandomAnd32Characters() {
        String secret = totp.newSecret();

        assertThat(secret).hasSize(32).matches("[A-Z2-7]+");
        assertThat(totp.newSecret()).isNotEqualTo(secret);
    }

    @Test
    void theQrTextHasTheIssuerTheAccountWithTheDateAndTheSecret() {
        String uri = totp.otpauthUri("ABCDEF", "luisa@bg.es", LocalDate.of(2026, 11, 22));

        assertThat(uri).startsWith("otpauth://totp/BG%20Sistemas%3Aluisa%40bg.es%20%C2%B7%2022%2F11%2F26?secret=ABCDEF&issuer=BG%20Sistemas")
                .contains("digits=6").contains("period=30");
    }

    // BGSI-337: the name the admin sees in the app, to know which entry to delete
    @Test
    void theEntryNameHasTheIssuerTheEmailAndTheDate() {
        assertThat(totp.entryName("luisa@bg.es", LocalDate.of(2026, 11, 22)))
                .isEqualTo("BG Sistemas \u00B7 luisa@bg.es \u00B7 22/11/26");
    }

    @Test
    void theEntryNameOfAnOldSecretHasNoDate() {
        assertThat(totp.entryName("luisa@bg.es", null)).isEqualTo("BG Sistemas \u00B7 luisa@bg.es");
    }
}
