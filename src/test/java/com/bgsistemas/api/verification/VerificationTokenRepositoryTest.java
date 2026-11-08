package com.bgsistemas.api.verification;

import com.bgsistemas.api.user.Role;
import com.bgsistemas.api.user.User;
import com.bgsistemas.api.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// BGSI-238: find the verification link by the hash of its token
@DataJpaTest
class VerificationTokenRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-11-08T10:00:00Z");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationTokenRepository verificationTokens;

    @Test
    void findsTheTokenByItsHash() {
        User admin = userRepository.save(new User("new-admin@test.com", "hashed", Role.ADMIN));
        verificationTokens.save(new VerificationToken(admin, "a".repeat(64), NOW.plus(Duration.ofHours(24))));

        Optional<VerificationToken> found = verificationTokens.findByTokenHash("a".repeat(64));

        assertThat(found).isPresent();
        assertThat(found.get().getUser().getEmail()).isEqualTo("new-admin@test.com");
    }

    @Test
    void returnsEmptyWhenTheHashDoesNotExist() {
        Optional<VerificationToken> found = verificationTokens.findByTokenHash("z".repeat(64));

        assertThat(found).isEmpty();
    }
}