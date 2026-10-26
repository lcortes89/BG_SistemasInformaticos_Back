package com.bgsistemas.api.brand;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    // SELECT * FROM brand WHERE active = true (public website)
    List<Brand> findByActiveTrue();

    // BGSI-302: admin list with search box (name or category) + active. null = no filter
    @Query("""
            SELECT b FROM Brand b
            WHERE (:active IS NULL OR b.active = :active)
              AND (:q IS NULL OR LOWER(b.name) LIKE :q ESCAPE '!' OR LOWER(COALESCE(b.category, '')) LIKE :q ESCAPE '!')""")
    Page<Brand> search(@Param("q") String q, @Param("active") Boolean active, Pageable pageable);

    // SELECT COUNT(*) FROM brand WHERE active = true
    long countByActiveTrue();
}
