package com.bgsistemas.api.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);

    // BGSI-291: the admins that receive the security alerts
    List<User> findByRole(Role role);

    // BGSI-315 + BGSI-302: "Administradores", one page at a time, with search box (email) + status.
    // null = no filter. A null status in the database means ACTIVE (old rows)
    @Query("""
            SELECT u FROM User u
            WHERE u.role = :role
              AND (:q IS NULL OR LOWER(u.email) LIKE :q ESCAPE '!')
              AND (:status IS NULL OR u.status = :status
                   OR (u.status IS NULL AND :status = com.bgsistemas.api.user.UserStatus.ACTIVE))""")
    Page<User> search(@Param("role") Role role, @Param("q") String q, @Param("status") UserStatus status,
                      Pageable pageable);
}
