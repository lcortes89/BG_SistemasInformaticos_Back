package com.bgsistemas.api.emailchange;

import com.bgsistemas.api.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface EmailChangeTokenRepository extends JpaRepository<EmailChangeToken, Long> {

    Optional<EmailChangeToken> findByTokenHash(String tokenHash);

    // Links still not used: a new request or a confirmation cancels them
    List<EmailChangeToken> findByUserAndUsedAtIsNull(User user);

    // Before deleting an admin (BGSI-277)
    @Transactional
    void deleteByUser(User user);
}
