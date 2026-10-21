package com.bgsistemas.api.twofactor;

import com.bgsistemas.api.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface RecoveryCodeRepository extends JpaRepository<RecoveryCode, Long> {

    List<RecoveryCode> findByUserAndUsedAtIsNull(User user);

    long countByUserAndUsedAtIsNull(User user);

    @Transactional
    void deleteByUser(User user);
}
