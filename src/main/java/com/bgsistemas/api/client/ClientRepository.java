package com.bgsistemas.api.client;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClientRepository extends JpaRepository<Client, Long> {

    // SELECT * FROM client WHERE published = true
    List<Client> findByPublishedTrue();

    // Same query, but one page at a time (LIMIT / OFFSET)
    Page<Client> findByPublishedTrue(Pageable pageable);

    // BGSI-302: admin list with search box (name or type of business) + active (= published). null = no filter
    @Query("""
            SELECT c FROM Client c
            WHERE (:active IS NULL OR c.published = :active)
              AND (:q IS NULL OR LOWER(c.name) LIKE :q ESCAPE '!' OR LOWER(COALESCE(c.businessType, '')) LIKE :q ESCAPE '!')""")
    Page<Client> search(@Param("q") String q, @Param("active") Boolean active, Pageable pageable);

    // SELECT COUNT(*) FROM client WHERE published = true
    long countByPublishedTrue();
}
