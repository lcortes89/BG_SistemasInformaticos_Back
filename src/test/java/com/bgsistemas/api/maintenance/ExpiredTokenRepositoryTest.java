package com.bgsistemas.api.maintenance;

import com.bgsistemas.api.emailchange.EmailChangeToken;
import com.bgsistemas.api.emailchange.EmailChangeTokenRepository;
import com.bgsistemas.api.passwordreset.PasswordResetToken;
import com.bgsistemas.api.passwordreset.PasswordResetTokenRepository;
import com.bgsistemas.api.user.Role;
import com.bgsistemas.api.user.User;
import com.bgsistemas.api.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

// BGSI-326: the DELETE queries of the nightly cleanup
@DataJpaTest
class ExpiredTokenRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-10-27T03:30:00Z");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokens;

    @Autowired
    private EmailChangeTokenRepository emailChangeTokens;

    private PasswordResetToken resetLink(User user, String hash, Instant expiresAt) {
        return passwordResetTokens.save(new PasswordResetToken(user, hash, expiresAt.minus(Duration.ofMinutes(30)), expiresAt));
    }

    @Test
    void deletesOnlyTheResetLinksThatExpiredBeforeTheMoment() {
        User admin = userRepository.save(new User("cleanup@test.com", "hashed", Role.ADMIN));
        resetLink(admin, "a".repeat(64), NOW.minus(Duration.ofDays(3)));
        resetLink(admin, "b".repeat(64), NOW.minus(Duration.ofDays(2)));
        PasswordResetToken recent = resetLink(admin, "c".repeat(64), NOW.minus(Duration.ofHours(2)));

        int deleted = passwordResetTokens.deleteExpiredBefore(NOW.minus(Duration.ofDays(1)));

        assertThat(deleted).isEqualTo(2);
        assertThat(passwordResetTokens.findAll()).extracting(PasswordResetToken::getId).containsExactly(recent.getId());
    }

    @Test
    void deletesOnlyTheEmailChangeLinksThatExpiredBeforeTheMoment() {
        User admin = userRepository.save(new User("cleanup2@test.com", "hashed", Role.ADMIN));
        emailChangeTokens.save(new EmailChangeToken(admin, "old@test.com", "d".repeat(64),
                NOW.minus(Duration.ofDays(3)), NOW.minus(Duration.ofDays(2))));
        EmailChangeToken valid = emailChangeTokens.save(new EmailChangeToken(admin, "new@test.com", "e".repeat(64),
                NOW.minus(Duration.ofMinutes(10)), NOW.plus(Duration.ofMinutes(50))));

        int deleted = emailChangeTokens.deleteExpiredBefore(NOW.minus(Duration.ofDays(1)));

        assertThat(deleted).isEqualTo(1);
        assertThat(emailChangeTokens.findAll()).extracting(EmailChangeToken::getId).containsExactly(valid.getId());
    }
}
