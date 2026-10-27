package com.bgsistemas.api.maintenance;

import com.bgsistemas.api.emailchange.EmailChangeTokenRepository;
import com.bgsistemas.api.passwordreset.PasswordResetTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

// BGSI-326: every night deletes the "forgot password", invitation and "change my email" links
// that expired more than a day ago (a used link also expires). They are no use any more,
// so the tables keep only the links that can still be opened
@Component
public class ExpiredTokenCleanup {

    private static final Logger log = LoggerFactory.getLogger(ExpiredTokenCleanup.class);

    // A day of margin: the "3 links every 15 minutes" limit still sees the recent ones
    static final Duration KEEP_AFTER_EXPIRY = Duration.ofDays(1);

    private final PasswordResetTokenRepository passwordResetTokens;
    private final EmailChangeTokenRepository emailChangeTokens;
    private final Clock clock;

    @Autowired
    public ExpiredTokenCleanup(PasswordResetTokenRepository passwordResetTokens,
                               EmailChangeTokenRepository emailChangeTokens) {
        this(passwordResetTokens, emailChangeTokens, Clock.systemUTC());
    }

    // The tests choose the time
    ExpiredTokenCleanup(PasswordResetTokenRepository passwordResetTokens,
                        EmailChangeTokenRepository emailChangeTokens, Clock clock) {
        this.passwordResetTokens = passwordResetTokens;
        this.emailChangeTokens = emailChangeTokens;
        this.clock = clock;
    }

    // Every day at 03:30 (app.cleanup.cron changes it; "-" turns it off). Returns how many links were deleted
    @Scheduled(cron = "${app.cleanup.cron:0 30 3 * * *}")
    @Transactional
    public int deleteExpiredTokens() {
        Instant before = Instant.now(clock).minus(KEEP_AFTER_EXPIRY);
        int deleted = passwordResetTokens.deleteExpiredBefore(before) + emailChangeTokens.deleteExpiredBefore(before);
        if (deleted > 0) {
            log.info("Deleted {} expired links", deleted);
        }
        return deleted;
    }
}
