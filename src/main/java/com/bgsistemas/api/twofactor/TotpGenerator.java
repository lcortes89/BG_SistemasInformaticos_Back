package com.bgsistemas.api.twofactor;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.OptionalLong;

// BGSI-276: TOTP codes (RFC 6238), the 6 digits that change every 30 seconds in the authenticator app.
// Written with the JDK only (HmacSHA1), so no extra library is needed
@Component
public class TotpGenerator {

    static final int DIGITS = 6;
    static final long PERIOD_SECONDS = 30;
    // One step before and after is also accepted: the clocks of the phone and the server are never exact
    static final int ALLOWED_STEPS = 1;
    static final String ISSUER = "BG Sistemas";

    private final SecureRandom random = new SecureRandom();

    // 20 random bytes (160 bits, the size RFC 4226 recommends) in Base32
    public String newSecret() {
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        return Base32.encode(bytes);
    }

    public long stepAt(Instant time) {
        return time.getEpochSecond() / PERIOD_SECONDS;
    }

    public String codeAt(String secret, long step) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(Base32.decode(secret), "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(step).array());
            // Dynamic truncation (RFC 4226, section 5.3)
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24) | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8) | (hash[offset + 3] & 0xFF);
            return String.format("%06d", binary % 1_000_000);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA1 is not available", e);
        }
    }

    // The step of the code if it is right now (or one step before/after), so it can be used only once
    public OptionalLong matchingStep(String secret, String code, Instant now) {
        if (code == null || !code.matches("\\d{" + DIGITS + "}")) {
            return OptionalLong.empty();
        }
        long current = stepAt(now);
        for (long step = current - ALLOWED_STEPS; step <= current + ALLOWED_STEPS; step++) {
            if (codeAt(secret, step).equals(code)) {
                return OptionalLong.of(step);
            }
        }
        return OptionalLong.empty();
    }

    // What the QR code contains: the app reads the issuer, the account and the secret
    public String otpauthUri(String secret, String email) {
        String label = encode(ISSUER + ":" + email);
        return "otpauth://totp/" + label + "?secret=" + secret + "&issuer=" + encode(ISSUER)
                + "&algorithm=SHA1&digits=" + DIGITS + "&period=" + PERIOD_SECONDS;
    }

    private static String encode(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
