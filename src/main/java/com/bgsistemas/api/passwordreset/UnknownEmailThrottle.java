package com.bgsistemas.api.passwordreset;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// BGSI-291: at most 3 "no account" emails per address every 15 minutes, so nobody can use
// the forgot password form to send spam. Kept in memory: there is no user row to count on.
@Component
public class UnknownEmailThrottle {

    static final Duration WINDOW = Duration.ofMinutes(15);
    static final int MAX_PER_WINDOW = 3;
    static final int MAX_TRACKED_EMAILS = 10_000;

    private final Map<String, Deque<Instant>> sentByEmail = new ConcurrentHashMap<>();

    // true = this email can be sent now (and it is counted); false = limit reached
    public boolean tryAcquire(String email, Instant now) {
        Instant since = now.minus(WINDOW);
        if (sentByEmail.size() >= MAX_TRACKED_EMAILS) {
            forgetOld(since);
        }
        Deque<Instant> times = sentByEmail.computeIfAbsent(normalize(email), key -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && !times.peekFirst().isAfter(since)) {
                times.pollFirst();
            }
            if (times.size() >= MAX_PER_WINDOW) {
                return false;
            }
            times.addLast(now);
            return true;
        }
    }

    int trackedEmails() {
        return sentByEmail.size();
    }

    private void forgetOld(Instant since) {
        sentByEmail.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                return entry.getValue().stream().noneMatch(time -> time.isAfter(since));
            }
        });
    }

    static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
