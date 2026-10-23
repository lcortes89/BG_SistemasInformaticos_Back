package com.bgsistemas.api.service;

import com.bgsistemas.api.brand.Brand;
import com.bgsistemas.api.brand.BrandRepository;
import com.bgsistemas.api.brand.exceptions.BrandNotFoundException;
import com.bgsistemas.api.service.exceptions.ServiceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ServiceServiceImpl implements ServiceService {

    private final ServiceRepository serviceRepository;
    private final BrandRepository brandRepository;

    public ServiceServiceImpl(ServiceRepository serviceRepository, BrandRepository brandRepository) {
        this.serviceRepository = serviceRepository;
        this.brandRepository = brandRepository;
    }

    @Override
    public List<Service> getAllServices() {
        return serviceRepository.findVisible();
    }

    @Override
    public List<Service> getServicesByBrandId(Long brandId) {
        return serviceRepository.findByBrandId(brandId);
    }

    @Override
    public List<Service> getFeaturedServices() {
        return serviceRepository.findVisibleFeatured();
    }

    @Override
    public Page<Service> getVisibleServicesPage(Long brandId, Pageable pageable) {
        return serviceRepository.findVisibleByBrandId(brandId, pageable);
    }

    @Override
    public Page<Service> getServicesPage(Long brandId, Pageable pageable) {
        if (brandId == null) {
            return serviceRepository.findAll(pageable);
        }
        return serviceRepository.findByBrandId(brandId, pageable);
    }

    @Override
    public Service getServiceById(Long id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> new ServiceNotFoundException(id));
    }

    @Override
    public Service createService(Service service, Long brandId) {
        service.setId(null); // always a new row
        service.setBrand(findBrand(brandId));
        return serviceRepository.save(service);
    }

    @Override
    public Service updateService(Long id, Service changes, Long brandId) {
        Service service = getServiceById(id);
        service.setName(changes.getName());
        service.setDescription(changes.getDescription());
        service.setImageUrl(changes.getImageUrl());
        service.setFeatured(changes.isFeatured());
        service.setLongDescription(changes.getLongDescription());
        service.setFeatures(changes.getFeatures());
        service.setSectors(changes.getSectors());
        service.setOfficialUrl(changes.getOfficialUrl());
        service.setActive(changes.isActive());
        service.setBrand(findBrand(brandId));
        return serviceRepository.save(service);
    }

    @Override
    public void deleteService(Long id) {
        serviceRepository.delete(getServiceById(id));
    }

    @Override
    public Service setServiceActive(Long id, boolean active) {
        Service service = getServiceById(id);
        service.setActive(active);
        return serviceRepository.save(service);
    }

    // Every service must belong to an existing brand
    private Brand findBrand(Long brandId) {
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new BrandNotFoundException(brandId));
    }
}
