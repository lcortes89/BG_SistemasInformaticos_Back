package com.bgsistemas.api.brand;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BrandService {

    // Public website: only active brands
    List<Brand> getActiveBrands();

    Brand getBrandById(Long id);

    // Admin panel
    Page<Brand> getBrandsPage(Pageable pageable);

    Brand createBrand(Brand brand);

    Brand updateBrand(Long id, Brand changes);

    void deleteBrand(Long id);

    long countServices(Long brandId);
}
