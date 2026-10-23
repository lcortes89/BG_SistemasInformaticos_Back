package com.bgsistemas.api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ServiceRepository extends JpaRepository<Service, Long> {

    List<Service> findByBrandId(Long brandId);

    // Same query, one page at a time (admin table filtered by brand)
    Page<Service> findByBrandId(Long brandId, Pageable pageable);

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

    // SELECT COUNT(*) FROM service WHERE brand_id = ?
    long countByBrandId(Long brandId);

    // SELECT COUNT(*) FROM service WHERE featured = true
    long countByFeaturedTrue();
}
