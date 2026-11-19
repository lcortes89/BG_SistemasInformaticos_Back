package com.bgsistemas.api.verification;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerificationTokenTest {

    private final Instant now = Instant.parse("2026-11-07T12:00:00Z");

    @Test
    void isUsable_whenNotUsedAndNotExpired_returnsTrue() {
        VerificationToken token = new VerificationToken(null, "hash", now.plusSeconds(3600));

        assertTrue(token.isUsable(now));
    }

    @Test
    void isUsable_whenExpired_returnsFalse() {
        VerificationToken token = new VerificationToken(null, "hash", now.minusSeconds(1));

        assertFalse(token.isUsable(now));
    }

    @Test
    void isUsable_afterMarkUsed_returnsFalse() {
        VerificationToken token = new VerificationToken(null, "hash", now.plusSeconds(3600));

        token.markUsed(now);

        assertFalse(token.isUsable(now));
        assertEquals(now, token.getUsedAt());
    }
}