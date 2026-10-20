package com.bgsistemas.api.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// BGSI-275: short tokens given after the admin types the password again.
// Valid for 5 minutes and only for the admin who asked for them. Kept in memory: after a restart
// the admin simply confirms the password again.
@Component
public class ConfirmationTokenService {

    public static final Duration VALIDITY = Duration.ofMinutes(5);
    static final int TOKEN_BYTES = 32;

    private record Confirmation(String email, Instant expiresAt) {
    }

    private final Map<String, Confirmation> confirmationsByToken = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public String issue(String email, Instant now) {
        forgetExpired(now);
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        confirmationsByToken.put(token, new Confirmation(LoginAttemptService.normalize(email), now.plus(VALIDITY)));
        return token;
    }

    public boolean isValid(String token, String email, Instant now) {
        if (token == null || token.isBlank() || email == null) {
            return false;
        }
        Confirmation confirmation = confirmationsByToken.get(token);
        return confirmation != null
                && confirmation.email().equals(LoginAttemptService.normalize(email))
                && confirmation.expiresAt().isAfter(now);
    }

    int activeTokens() {
        return confirmationsByToken.size();
    }

    private void forgetExpired(Instant now) {
        confirmationsByToken.values().removeIf(confirmation -> !confirmation.expiresAt().isAfter(now));
    }
}
