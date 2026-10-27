package com.bgsistemas.api.maintenance;

import com.bgsistemas.api.emailchange.EmailChangeTokenRepository;
import com.bgsistemas.api.passwordreset.PasswordResetTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// BGSI-326: the nightly cleanup of the old links
class ExpiredTokenCleanupTest {

    private static final Instant NOW = Instant.parse("2026-10-27T03:30:00Z");

    private final PasswordResetTokenRepository passwordResetTokens = mock(PasswordResetTokenRepository.class);
    private final EmailChangeTokenRepository emailChangeTokens = mock(EmailChangeTokenRepository.class);
    private final ExpiredTokenCleanup cleanup =
            new ExpiredTokenCleanup(passwordResetTokens, emailChangeTokens, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void deletesTheLinksThatExpiredMoreThanADayAgo() {
        Instant oneDayAgo = Instant.parse("2026-10-26T03:30:00Z");
        when(passwordResetTokens.deleteExpiredBefore(oneDayAgo)).thenReturn(3);
        when(emailChangeTokens.deleteExpiredBefore(oneDayAgo)).thenReturn(2);

        int deleted = cleanup.deleteExpiredTokens();

        assertThat(deleted).isEqualTo(5);
        verify(passwordResetTokens).deleteExpiredBefore(oneDayAgo);
        verify(emailChangeTokens).deleteExpiredBefore(oneDayAgo);
    }

    @Test
    void runsEveryNightInOneTransaction() throws NoSuchMethodException {
        var method = ExpiredTokenCleanup.class.getMethod("deleteExpiredTokens");

        assertThat(method.getAnnotation(Scheduled.class).cron()).isEqualTo("${app.cleanup.cron:0 30 3 * * *}");
        assertThat(method.getAnnotation(Transactional.class)).isNotNull();
    }
}
