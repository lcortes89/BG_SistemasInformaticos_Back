package com.bgsistemas.api.service;

import com.bgsistemas.api.globals.BulkResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ServiceService {

    // Public website: only active products of active brands (BGSI-309)
    List<Service> getAllServices();

    List<Service> getServicesByBrandId(Long brandId);

    List<Service> getFeaturedServices();

    Page<Service> getVisibleServicesPage(Long brandId, Pageable pageable);

    // Admin panel (brandId = null -> all brands). BGSI-302: search = product or brand name, active. null = no filter
    Page<Service> getServicesPage(Long brandId, String search, Boolean active, Pageable pageable);

    Service getServiceById(Long id);

    Service createService(Service service, Long brandId);

    Service updateService(Long id, Service changes, Long brandId);

    void deleteService(Long id);

    // BGSI-309: show or hide the product on the website without deleting it
    Service setServiceActive(Long id, boolean active);

    // BGSI-333: bulk actions (the missing ids are reported, not an error)
    BulkResult deleteServices(List<Long> ids);

    BulkResult setServicesActive(List<Long> ids, boolean active);
}
