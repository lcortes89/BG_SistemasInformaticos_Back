package com.bgsistemas.api.passwordreset;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

// BGSI-291: no more than 3 "no account" emails per address every 15 minutes
class UnknownEmailThrottleTest {

    private final UnknownEmailThrottle throttle = new UnknownEmailThrottle();
    private final Instant now = Instant.parse("2026-10-20T12:00:00Z");

    @Test
    void allowsThreeEmailsAndBlocksTheFourth() {
        assertThat(throttle.tryAcquire("nobody@mail.com", now)).isTrue();
        assertThat(throttle.tryAcquire("nobody@mail.com", now.plusSeconds(60))).isTrue();
        assertThat(throttle.tryAcquire("nobody@mail.com", now.plusSeconds(120))).isTrue();

        assertThat(throttle.tryAcquire("nobody@mail.com", now.plusSeconds(180))).isFalse();
    }

    @Test
    void countsTheSameEmailWithSpacesOrCapitalLetters() {
        throttle.tryAcquire("nobody@mail.com", now);
        throttle.tryAcquire(" NOBODY@mail.com", now);
        throttle.tryAcquire("Nobody@Mail.com ", now);

        assertThat(throttle.tryAcquire("nobody@mail.com", now)).isFalse();
        assertThat(throttle.tryAcquire("other@mail.com", now)).isTrue();
    }

    @Test
    void allowsAgainWhenFifteenMinutesHavePassed() {
        for (int i = 0; i < 3; i++) {
            throttle.tryAcquire("nobody@mail.com", now);
        }

        assertThat(throttle.tryAcquire("nobody@mail.com", now.plus(UnknownEmailThrottle.WINDOW))).isTrue();
    }

    @Test
    void forgetsOldEmailsWhenItTracksTooMany() {
        for (int i = 0; i < UnknownEmailThrottle.MAX_TRACKED_EMAILS; i++) {
            throttle.tryAcquire("user" + i + "@mail.com", now);
        }
        assertThat(throttle.trackedEmails()).isEqualTo(UnknownEmailThrottle.MAX_TRACKED_EMAILS);

        Instant later = now.plus(UnknownEmailThrottle.WINDOW).plusSeconds(1);
        assertThat(throttle.tryAcquire("new@mail.com", later)).isTrue();

        assertThat(throttle.trackedEmails()).isEqualTo(1);
    }
}
