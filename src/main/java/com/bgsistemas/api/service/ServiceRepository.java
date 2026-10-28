package com.bgsistemas.api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ServiceRepository extends JpaRepository<Service, Long> {

    List<Service> findByBrandId(Long brandId);

    // SELECT * FROM service WHERE featured = true
    List<Service> findByFeaturedTrue();

    // BGSI-309: public website. Only active products of active brands (null = active, old rows)
    @Query("""
            SELECT s FROM Service s LEFT JOIN s.brand b
            WHERE (s.active IS NULL OR s.active = true) AND (b IS NULL OR b.active = true)
            ORDER BY s.id""")
    List<Service> findVisible();

    @Query("""
            SELECT s FROM Service s LEFT JOIN s.brand b
            WHERE s.featured = true AND (s.active IS NULL OR s.active = true) AND (b IS NULL OR b.active = true)
            ORDER BY s.id""")
    List<Service> findVisibleFeatured();

    @Query("""
            SELECT s FROM Service s JOIN s.brand b
            WHERE b.id = :brandId AND b.active = true AND (s.active IS NULL OR s.active = true)""")
    Page<Service> findVisibleByBrandId(@Param("brandId") Long brandId, Pageable pageable);

    // BGSI-302: admin list with brand + search box (product or brand name) + active (null = active, old rows).
    // Every parameter can be null = no filter
    @Query("""
            SELECT s FROM Service s LEFT JOIN s.brand b
            WHERE (:brandId IS NULL OR b.id = :brandId)
              AND (:active IS NULL
                   OR (:active = true AND (s.active IS NULL OR s.active = true))
                   OR (:active = false AND s.active = false))
              AND (:q IS NULL OR LOWER(s.name) LIKE :q ESCAPE '!' OR LOWER(COALESCE(b.name, '')) LIKE :q ESCAPE '!')""")
    Page<Service> search(@Param("brandId") Long brandId, @Param("q") String q, @Param("active") Boolean active,
                         Pageable pageable);

    // SELECT COUNT(*) FROM service WHERE brand_id = ?
    long countByBrandId(Long brandId);

    // SELECT COUNT(*) FROM service WHERE featured = true
    long countByFeaturedTrue();
}
