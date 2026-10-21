package com.bgsistemas.api.twofactor;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

// BGSI-276: first step of the login done (right password) and waiting for the 6-digit code.
// The pending token is not a JWT: it does not open the panel, it only says which admin is in the middle of the login
@Component
public class PendingLoginService {

    public static final Duration VALIDITY = Duration.ofMinutes(5);

    private record Pending(String email, Instant expiresAt) {
    }

    private final Map<String, Pending> pendingByToken = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public String start(String email, Instant now) {
        pendingByToken.values().removeIf(pending -> !pending.expiresAt().isAfter(now));
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        pendingByToken.put(token, new Pending(email, now.plus(VALIDITY)));
        return token;
    }

    public Optional<String> emailFor(String token, Instant now) {
        Pending pending = token == null ? null : pendingByToken.get(token);
        if (pending == null || !pending.expiresAt().isAfter(now)) {
            return Optional.empty();
        }
        return Optional.of(pending.email());
    }

    public void finish(String token) {
        pendingByToken.remove(token);
    }
}
