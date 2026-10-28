package com.bgsistemas.api.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

// BGSI-274: after 5 failed logins in a row with the same email, that email is locked for 15 minutes.
// Kept in memory and counted for ANY email (registered or not), so the lock does not reveal
// which emails are admins.
@Component
public class LoginAttemptService {

    static final int MAX_FAILURES = 5;
    static final Duration LOCK_TIME = Duration.ofMinutes(15);
    static final int MAX_TRACKED_EMAILS = 10_000;

    // failures: wrong passwords in a row; lastFailureAt: the last one; lockedUntil: null = not locked
    private record Attempts(int failures, Instant lastFailureAt, Instant lockedUntil) {
    }

    private final Map<String, Attempts> attemptsByEmail = new ConcurrentHashMap<>();

    // How long the email is still locked (empty = it can try to log in)
    public Optional<Duration> lockRemaining(String email, Instant now) {
        Attempts attempts = attemptsByEmail.get(normalize(email));
        if (attempts == null || attempts.lockedUntil() == null || !attempts.lockedUntil().isAfter(now)) {
            return Optional.empty();
        }
        return Optional.of(Duration.between(now, attempts.lockedUntil()));
    }

    public void onFailure(String email, Instant now) {
        if (attemptsByEmail.size() >= MAX_TRACKED_EMAILS) {
            forgetOld(now);
        }
        attemptsByEmail.compute(normalize(email), (key, previous) -> {
            // Old failures (more than 15 minutes ago) or a lock that already ended do not count
            boolean recent = previous != null && previous.lastFailureAt().isAfter(now.minus(LOCK_TIME));
            int failures = recent ? previous.failures() + 1 : 1;
            if (failures >= MAX_FAILURES) {
                return new Attempts(0, now, now.plus(LOCK_TIME));
            }
            return new Attempts(failures, now, null);
        });
    }

    // A correct password before the 5th failure starts the count again
    public void onSuccess(String email) {
        attemptsByEmail.remove(normalize(email));
    }

    int trackedEmails() {
        return attemptsByEmail.size();
    }

    private void forgetOld(Instant now) {
        Instant since = now.minus(LOCK_TIME);
        attemptsByEmail.values().removeIf(attempts -> !attempts.lastFailureAt().isAfter(since)
                && (attempts.lockedUntil() == null || !attempts.lockedUntil().isAfter(now)));
    }

    static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
