package com.bgsistemas.api.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

// BGSI-274: 5 wrong passwords in a row -> 15 minutes locked
class LoginAttemptServiceTest {

    private final LoginAttemptService service = new LoginAttemptService();
    private final Instant now = Instant.parse("2026-10-20T12:00:00Z");

    private void fail(String email, int times, Instant at) {
        for (int i = 0; i < times; i++) {
            service.onFailure(email, at);
        }
    }

    @Test
    void locksTheEmailFor15MinutesAfterTheFifthFailure() {
        fail("admin@mail.com", 4, now);
        assertThat(service.lockRemaining("admin@mail.com", now)).isEmpty();

        service.onFailure("admin@mail.com", now);

        assertThat(service.lockRemaining("admin@mail.com", now)).contains(Duration.ofMinutes(15));
        assertThat(service.lockRemaining("admin@mail.com", now.plusSeconds(600))).contains(Duration.ofMinutes(5));
    }

    @Test
    void unlocksWhenTheTimeHasPassed() {
        fail("admin@mail.com", 5, now);

        Instant later = now.plus(LoginAttemptService.LOCK_TIME);
        assertThat(service.lockRemaining("admin@mail.com", later)).isEmpty();

        service.onFailure("admin@mail.com", later.plusSeconds(1)); // starts again from 1
        assertThat(service.lockRemaining("admin@mail.com", later.plusSeconds(1))).isEmpty();
    }

    @Test
    void aCorrectLoginStartsTheCountAgain() {
        fail("admin@mail.com", 4, now);
        service.onSuccess("admin@mail.com");
        fail("admin@mail.com", 4, now);

        assertThat(service.lockRemaining("admin@mail.com", now)).isEmpty();
    }

    @Test
    void oldFailuresDoNotCount() {
        fail("admin@mail.com", 4, now);

        service.onFailure("admin@mail.com", now.plus(LoginAttemptService.LOCK_TIME).plusSeconds(1));

        assertThat(service.lockRemaining("admin@mail.com", now.plus(LoginAttemptService.LOCK_TIME))).isEmpty();
    }

    @Test
    void countsTheSameEmailWithSpacesOrCapitalLetters() {
        fail("admin@mail.com", 2, now);
        fail(" ADMIN@mail.com", 2, now);
        service.onFailure("Admin@Mail.com ", now);

        assertThat(service.lockRemaining("admin@mail.com", now)).isPresent();
        assertThat(service.lockRemaining("other@mail.com", now)).isEmpty();
    }

    @Test
    void forgetsOldEmailsWhenItTracksTooMany() {
        for (int i = 0; i < LoginAttemptService.MAX_TRACKED_EMAILS; i++) {
            service.onFailure("user" + i + "@mail.com", now);
        }
        fail("locked@mail.com", 5, now.plusSeconds(60));

        service.onFailure("new@mail.com", now.plus(LoginAttemptService.LOCK_TIME).plusSeconds(1));

        assertThat(service.trackedEmails()).isEqualTo(2); // the locked one and the new one
        assertThat(service.lockRemaining("locked@mail.com", now.plus(LoginAttemptService.LOCK_TIME))).isPresent();
    }
}
