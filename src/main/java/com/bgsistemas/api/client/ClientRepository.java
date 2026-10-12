package com.bgsistemas.api.client;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClientRepository extends JpaRepository<Client, Long> {

    // SELECT * FROM client WHERE published = true
    List<Client> findByPublishedTrue();

    // Same query, but one page at a time (LIMIT / OFFSET)
    Page<Client> findByPublishedTrue(Pageable pageable);

    // SELECT COUNT(*) FROM client WHERE published = true
    long countByPublishedTrue();
}
