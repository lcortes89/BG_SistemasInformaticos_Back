package com.bgsistemas.api.brand;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BrandService {

    // Public website: only active brands
    List<Brand> getActiveBrands();

    Brand getBrandById(Long id);

    // Admin panel
    // BGSI-302: search = name or category, active. null = no filter
    Page<Brand> getBrandsPage(String search, Boolean active, Pageable pageable);

    Brand createBrand(Brand brand);

    Brand updateBrand(Long id, Brand changes);

    void deleteBrand(Long id);

    long countServices(Long brandId);

    // BGSI-309: show or hide the brand (and its products) on the website without deleting it
    Brand setBrandActive(Long id, boolean active);
}
