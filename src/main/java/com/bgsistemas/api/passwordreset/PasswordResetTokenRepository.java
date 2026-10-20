package com.bgsistemas.api.passwordreset;

import com.bgsistemas.api.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    // How many links were asked for this user since a moment (limit of emails)
    long countByUserAndCreatedAtAfter(User user, Instant since);

    // Links still not used: they are cancelled when the password changes
    List<PasswordResetToken> findByUserAndUsedAtIsNull(User user);
}
