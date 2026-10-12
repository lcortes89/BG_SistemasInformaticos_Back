package com.bgsistemas.api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ServiceService {

    // Public website
    List<Service> getAllServices();

    List<Service> getServicesByBrandId(Long brandId);

    List<Service> getFeaturedServices();

    // Admin panel (brandId = null -> all brands)
    Page<Service> getServicesPage(Long brandId, Pageable pageable);

    Service getServiceById(Long id);

    Service createService(Service service, Long brandId);

    Service updateService(Long id, Service changes, Long brandId);

    void deleteService(Long id);
}
