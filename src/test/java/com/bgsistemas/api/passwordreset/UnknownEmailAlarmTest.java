package com.bgsistemas.api.passwordreset;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// BGSI-291: many requests with unknown emails -> one alert for the admins
class UnknownEmailAlarmTest {

    private final UnknownEmailAlarm alarm = new UnknownEmailAlarm();
    private final Instant now = Instant.parse("2026-10-20T12:00:00Z");

    private Optional<UnknownEmailAlarm.Alert> recordMany(int count, Instant at) {
        Optional<UnknownEmailAlarm.Alert> last = Optional.empty();
        for (int i = 0; i < count; i++) {
            Optional<UnknownEmailAlarm.Alert> alert = alarm.record("user" + i + "@mail.com", at);
            if (alert.isPresent()) {
                last = alert;
            }
        }
        return last;
    }

    @Test
    void doesNotAlertForAFewMistakes() {
        assertThat(recordMany(UnknownEmailAlarm.THRESHOLD - 1, now)).isEmpty();
    }

    @Test
    void alertsWhenTheLimitIsReachedWithTheEmailsUsed() {
        recordMany(UnknownEmailAlarm.THRESHOLD - 1, now);

        Optional<UnknownEmailAlarm.Alert> alert = alarm.record(" Luisa@Hotmal.com ", now);

        assertThat(alert).isPresent();
        assertThat(alert.get().attempts()).isEqualTo(UnknownEmailAlarm.THRESHOLD);
        assertThat(alert.get().emails()).contains("luisa@hotmal.com", "user0@mail.com");
    }

    @Test
    void sendsOnlyOneAlertEveryFifteenMinutes() {
        recordMany(UnknownEmailAlarm.THRESHOLD, now);

        assertThat(alarm.record("again@mail.com", now.plusSeconds(60))).isEmpty();

        Instant later = now.plus(UnknownEmailAlarm.WINDOW).plusSeconds(1);
        assertThat(recordMany(UnknownEmailAlarm.THRESHOLD, later)).isPresent();
    }

    @Test
    void forgetsTheRequestsOlderThanFifteenMinutes() {
        recordMany(UnknownEmailAlarm.THRESHOLD - 1, now);

        Instant later = now.plus(UnknownEmailAlarm.WINDOW);
        assertThat(alarm.record("one-more@mail.com", later)).isEmpty();
    }

    @Test
    void keepsAMemoryLimitAndAShortListOfEmails() {
        Optional<UnknownEmailAlarm.Alert> alert = Optional.empty();
        for (int i = 0; i <= UnknownEmailAlarm.MAX_TRACKED; i++) {
            Optional<UnknownEmailAlarm.Alert> result = alarm.record("bot" + i + "@mail.com", now.plusMillis(i));
            if (i == UnknownEmailAlarm.THRESHOLD - 1) {
                alert = result;
            }
        }

        assertThat(alert).isPresent();
        assertThat(alert.get().emails()).hasSizeLessThanOrEqualTo(UnknownEmailAlarm.MAX_EMAILS_IN_ALERT);
        assertThat(alarm.record("last@mail.com", now.plusSeconds(120))).isEmpty();
    }
}
