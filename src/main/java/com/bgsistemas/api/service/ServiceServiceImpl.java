package com.bgsistemas.api.service;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ServiceServiceImpl implements ServiceService {

    private final ServiceRepository serviceRepository;

    public ServiceServiceImpl(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    @Override
    public List<Service> getAllServices() {
        return serviceRepository.findAll();
    }

    @Override
    public List<Service> getServicesByBrandId(Long brandId) {
        return serviceRepository.findByBrandId(brandId);
    }

    // NEW
    @Override
    public List<Service> getFeaturedServices() {
        return serviceRepository.findByFeaturedTrue();
    }
}