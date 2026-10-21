package com.bgsistemas.api.twofactor;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

// BGSI-276: right password, waiting for the code (5 minutes)
class PendingLoginServiceTest {

    private final PendingLoginService service = new PendingLoginService();
    private final Instant now = Instant.parse("2026-10-21T10:00:00Z");

    @Test
    void remembersWhoIsLoggingInForFiveMinutes() {
        String token = service.start("luisa@bg.es", now);

        assertThat(service.emailFor(token, now.plusSeconds(299))).contains("luisa@bg.es");
        assertThat(service.emailFor(token, now.plus(PendingLoginService.VALIDITY))).isEmpty();
    }

    @Test
    void aFinishedOrMadeUpTokenIsNotValid() {
        String token = service.start("luisa@bg.es", now);
        service.finish(token);

        assertThat(service.emailFor(token, now)).isEmpty();
        assertThat(service.emailFor("made-up", now)).isEmpty();
        assertThat(service.emailFor(null, now)).isEmpty();
    }
}
