package com.bgsistemas.api.passwordreset;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

// BGSI-291: counts the "¿Olvidaste tu contraseña?" requests made with emails that are not an admin.
// Many of them in a short time can be an attack, so the admins get ONE summary email
// (and no more than one every 15 minutes, so a bot cannot fill their inbox).
@Component
public class UnknownEmailAlarm {

    static final Duration WINDOW = Duration.ofMinutes(15);
    static final int THRESHOLD = 10;
    static final int MAX_TRACKED = 1_000; // memory limit if a bot sends thousands of requests
    static final int MAX_EMAILS_IN_ALERT = 20;

    private final Deque<Attempt> attempts = new ArrayDeque<>();
    private Instant lastAlertAt;

    // What the admins receive: how many requests and with which emails
    public record Alert(int attempts, List<String> emails) {
    }

    private record Attempt(String email, Instant at) {
    }

    // Saves one request. Returns the alert only when the limit is reached and no alert was sent recently
    public synchronized Optional<Alert> record(String email, Instant now) {
        Instant since = now.minus(WINDOW);
        while (!attempts.isEmpty() && !attempts.peekFirst().at().isAfter(since)) {
            attempts.pollFirst();
        }
        if (attempts.size() >= MAX_TRACKED) {
            attempts.pollFirst();
        }
        attempts.addLast(new Attempt(normalize(email), now));

        boolean alertedRecently = lastAlertAt != null && lastAlertAt.isAfter(since);
        if (attempts.size() < THRESHOLD || alertedRecently) {
            return Optional.empty();
        }
        lastAlertAt = now;
        List<String> emails = attempts.stream().map(Attempt::email).distinct().limit(MAX_EMAILS_IN_ALERT).toList();
        return Optional.of(new Alert(attempts.size(), emails));
    }

    static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
