package com.bgsistemas.api.contact;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    // SELECT * FROM contact WHERE status = ? (LIMIT / OFFSET)
    Page<Contact> findByStatus(ContactStatus status, Pageable pageable);
}
