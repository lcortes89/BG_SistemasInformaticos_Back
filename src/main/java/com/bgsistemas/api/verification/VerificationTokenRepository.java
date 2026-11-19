package com.bgsistemas.api.verification;

import com.bgsistemas.api.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    Optional<VerificationToken> findByTokenHash(String tokenHash);

    // BGSI-238: links of this admin that have not expired yet (limit for the resend)
    long countByUserAndExpiresAtAfter(User user, Instant now);
}
