package com.bgsistemas.api.brand;

import com.bgsistemas.api.brand.exceptions.BrandInUseException;
import com.bgsistemas.api.brand.exceptions.BrandNotFoundException;
import com.bgsistemas.api.globals.BulkResult;
import com.bgsistemas.api.globals.SearchText;
import com.bgsistemas.api.service.ServiceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    @Transactional(readOnly = true) // BGSI-326: only reads
    public List<Brand> getActiveBrands() {
        return brandRepository.findByActiveTrue();
    }

    @Override
    @Transactional(readOnly = true) // BGSI-326: only reads
    public Brand getBrandById(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new BrandNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true) // BGSI-326: only reads
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
    @Transactional(readOnly = true) // BGSI-326: only reads
    public long countServices(Long brandId) {
        return serviceRepository.countByBrandId(brandId);
    }

    @Override
    public Brand setBrandActive(Long id, boolean active) {
        Brand brand = getBrandById(id);
        brand.setActive(active);
        return brandRepository.save(brand);
    }

    // BGSI-333: same rule as deleteBrand, but one brand that cannot be deleted does not stop the others
    @Override
    @Transactional
    public BulkResult deleteBrands(List<Long> ids) {
        List<Long> unique = ids.stream().distinct().toList();
        List<Brand> toDelete = new ArrayList<>();
        List<BulkResult.Failure> refused = new ArrayList<>();
        for (Brand brand : brandRepository.findAllById(unique)) {
            if (serviceRepository.countByBrandId(brand.getId()) > 0) {
                refused.add(new BulkResult.Failure(brand.getId(), BulkResult.HAS_PRODUCTS));
            } else {
                toDelete.add(brand);
            }
        }
        brandRepository.deleteAll(toDelete);
        return BulkResult.of(unique, toDelete.stream().map(Brand::getId).toList(), refused);
    }

    @Override
    @Transactional
    public BulkResult setBrandsActive(List<Long> ids, boolean active) {
        List<Long> unique = ids.stream().distinct().toList();
        List<Brand> found = brandRepository.findAllById(unique);
        found.forEach(brand -> brand.setActive(active));
        brandRepository.saveAll(found);
        return BulkResult.of(unique, found.stream().map(Brand::getId).toList());
    }
}
