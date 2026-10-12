package com.bgsistemas.api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceRepository extends JpaRepository<Service, Long> {

    List<Service> findByBrandId(Long brandId);

    // Same query, one page at a time (admin table filtered by brand)
    Page<Service> findByBrandId(Long brandId, Pageable pageable);

    // SELECT * FROM service WHERE featured = true
    List<Service> findByFeaturedTrue();

    // SELECT COUNT(*) FROM service WHERE brand_id = ?
    long countByBrandId(Long brandId);

    // SELECT COUNT(*) FROM service WHERE featured = true
    long countByFeaturedTrue();
}
