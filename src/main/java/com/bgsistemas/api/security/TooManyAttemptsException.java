package com.bgsistemas.api.security;

import java.time.Duration;

// BGSI-275: 429 when the email is locked (5 wrong passwords, see LoginAttemptService)
public class TooManyAttemptsException extends RuntimeException {

    private final long retryAfterSeconds;
    private final long retryAfterMinutes;

    public TooManyAttemptsException(Duration remaining) {
        super("Too many failed attempts. Try again in " + minutes(remaining) + " minutes");
        this.retryAfterSeconds = Math.max(1, (remaining.toMillis() + 999) / 1000); // rounded up
        this.retryAfterMinutes = minutes(remaining);
    }

    // Rounded up: never "0 minutes"
    private static long minutes(Duration remaining) {
        long seconds = (remaining.toMillis() + 999) / 1000;
        return Math.max(1, (seconds + 59) / 60);
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public long getRetryAfterMinutes() {
        return retryAfterMinutes;
    }
}
