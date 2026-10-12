package com.bgsistemas.api.service;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceRepository extends JpaRepository<Service, Long> {

    List<Service> findByBrandId(Long brandId);

    // SELECT * FROM service WHERE featured = true
    List<Service> findByFeaturedTrue();

    // SELECT COUNT(*) FROM service WHERE brand_id = ?
    long countByBrandId(Long brandId);
}
