package com.bgsistemas.api.passwordreset;

import com.bgsistemas.api.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    // How many links were asked for this user since a moment (limit of emails)
    long countByUserAndCreatedAtAfter(User user, Instant since);

    // Links still not used: they are cancelled when the password changes
    List<PasswordResetToken> findByUserAndUsedAtIsNull(User user);

    // BGSI-277: before deleting an admin
    @Transactional
    void deleteByUser(User user);

    // BGSI-326: the nightly cleanup. Links that expired before that moment (used links also expire)
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :before")
    int deleteExpiredBefore(@Param("before") Instant before);
}
