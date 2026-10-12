package com.bgsistemas.api.contact;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    // SELECT * FROM contact WHERE status = ? (LIMIT / OFFSET)
    Page<Contact> findByStatus(ContactStatus status, Pageable pageable);

    // SELECT COUNT(*) FROM contact WHERE status = ?
    long countByStatus(ContactStatus status);

    // SELECT * FROM contact ORDER BY created_at DESC LIMIT 5
    List<Contact> findTop5ByOrderByCreatedAtDesc();
}
