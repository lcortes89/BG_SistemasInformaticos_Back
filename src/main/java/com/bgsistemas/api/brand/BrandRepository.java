package com.bgsistemas.api.brand;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    // SELECT * FROM brand WHERE active = true (public website)
    List<Brand> findByActiveTrue();
}
