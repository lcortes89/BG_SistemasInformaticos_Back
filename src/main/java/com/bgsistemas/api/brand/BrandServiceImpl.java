package com.bgsistemas.api.brand;

import com.bgsistemas.api.brand.exceptions.BrandInUseException;
import com.bgsistemas.api.brand.exceptions.BrandNotFoundException;
import com.bgsistemas.api.globals.SearchText;
import com.bgsistemas.api.service.ServiceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final ServiceRepository serviceRepository;

    public BrandServiceImpl(BrandRepository brandRepository, ServiceRepository serviceRepository) {
        this.brandRepository = brandRepository;
        this.serviceRepository = serviceRepository;
    }

    @Override
    public List<Brand> getActiveBrands() {
        return brandRepository.findByActiveTrue();
    }

    @Override
    public Brand getBrandById(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new BrandNotFoundException(id));
    }

    @Override
    public Page<Brand> getBrandsPage(String search, Boolean active, Pageable pageable) {
        return brandRepository.search(SearchText.likePattern(search), active, pageable);
    }

    @Override
    public Brand createBrand(Brand brand) {
        brand.setId(null); // always a new row
        return brandRepository.save(brand);
    }

    @Override
    public Brand updateBrand(Long id, Brand changes) {
        Brand brand = getBrandById(id);
        brand.setName(changes.getName());
        brand.setDescription(changes.getDescription());
        brand.setCategory(changes.getCategory());
        brand.setLogoUrl(changes.getLogoUrl());
        brand.setActive(changes.isActive());
        return brandRepository.save(brand);
    }

    @Override
    public void deleteBrand(Long id) {
        Brand brand = getBrandById(id);
        // Business rule: a brand with services cannot be deleted (no orphan services)
        long services = serviceRepository.countByBrandId(id);
        if (services > 0) {
            throw new BrandInUseException(brand.getName(), services);
        }
        brandRepository.delete(brand);
    }

    @Override
    public long countServices(Long brandId) {
        return serviceRepository.countByBrandId(brandId);
    }

    @Override
    public Brand setBrandActive(Long id, boolean active) {
        Brand brand = getBrandById(id);
        brand.setActive(active);
        return brandRepository.save(brand);
    }
}
