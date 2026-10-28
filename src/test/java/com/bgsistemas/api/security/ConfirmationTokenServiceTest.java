package com.bgsistemas.api.security;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

// BGSI-275: confirmation tokens of 5 minutes, only for the admin who asked for them
class ConfirmationTokenServiceTest {

    private final ConfirmationTokenService service = new ConfirmationTokenService();
    private final Instant now = Instant.parse("2026-10-20T12:00:00Z");

    @Test
    void theTokenWorksForFiveMinutes() {
        String token = service.issue("admin@mail.com", now);

        assertThat(service.isValid(token, "admin@mail.com", now)).isTrue();
        assertThat(service.isValid(token, "admin@mail.com", now.plusSeconds(299))).isTrue();
        assertThat(service.isValid(token, "admin@mail.com", now.plus(ConfirmationTokenService.VALIDITY))).isFalse();
    }

    @Test
    void theTokenOnlyWorksForTheAdminWhoConfirmed() {
        String token = service.issue("admin@mail.com", now);

        assertThat(service.isValid(token, "other@mail.com", now)).isFalse();
        assertThat(service.isValid(token, " ADMIN@mail.com ", now)).isTrue();
    }

    @Test
    void madeUpOrMissingTokensAreNotValid() {
        service.issue("admin@mail.com", now);

        assertThat(service.isValid("made-up", "admin@mail.com", now)).isFalse();
        assertThat(service.isValid(null, "admin@mail.com", now)).isFalse();
        assertThat(service.isValid("", "admin@mail.com", now)).isFalse();
        assertThat(service.isValid("made-up", null, now)).isFalse();
    }

    @Test
    void everyConfirmationGetsADifferentToken() {
        assertThat(service.issue("admin@mail.com", now)).isNotEqualTo(service.issue("admin@mail.com", now));
    }

    @Test
    void expiredTokensAreForgotten() {
        service.issue("admin@mail.com", now);
        service.issue("other@mail.com", now);

        service.issue("admin@mail.com", now.plus(ConfirmationTokenService.VALIDITY));

        assertThat(service.activeTokens()).isEqualTo(1);
    }
}
