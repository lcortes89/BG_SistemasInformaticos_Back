package com.bgsistemas.api.contact;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    // BGSI-302: search box (name, email or product) + status. null = no filter
    @Query("""
            SELECT c FROM Contact c
            WHERE (:status IS NULL OR c.status = :status)
              AND (:q IS NULL OR LOWER(c.name) LIKE :q ESCAPE '!' OR LOWER(c.email) LIKE :q ESCAPE '!'
                   OR LOWER(COALESCE(c.interestedService, '')) LIKE :q ESCAPE '!')""")
    Page<Contact> search(@Param("status") ContactStatus status, @Param("q") String q, Pageable pageable);

    // SELECT COUNT(*) FROM contact WHERE status = ?
    long countByStatus(ContactStatus status);

    // SELECT * FROM contact ORDER BY created_at DESC LIMIT 5
    List<Contact> findTop5ByOrderByCreatedAtDesc();
}
