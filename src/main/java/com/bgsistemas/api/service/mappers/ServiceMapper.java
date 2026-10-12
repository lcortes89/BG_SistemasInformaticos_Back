package com.bgsistemas.api.service.mappers;

import com.bgsistemas.api.brand.Brand;
import com.bgsistemas.api.service.Service;
import com.bgsistemas.api.service.dtos.AdminServiceDTO;
import com.bgsistemas.api.service.dtos.ServiceDTO;
import com.bgsistemas.api.service.dtos.ServiceRequestDTO;
import org.springframework.stereotype.Component;

@Component
public class ServiceMapper {

    // Entity -> public API response
    public ServiceDTO toDTO(Service service) {
        Brand brand = service.getBrand();

        return new ServiceDTO(
                service.getId(),
                service.getName(),
                service.getDescription(),
                service.getImageUrl(),
                brand != null ? brand.getId() : null,
                brand != null ? brand.getName() : null
        );
    }

    // Entity -> admin panel response
    public AdminServiceDTO toAdminDTO(Service service) {
        Brand brand = service.getBrand();

        return new AdminServiceDTO(
                service.getId(),
                service.getName(),
                service.getDescription(),
                service.getImageUrl(),
                service.isFeatured(),
                brand != null ? brand.getId() : null,
                brand != null ? brand.getName() : null
        );
    }

    // Admin form -> entity (the brand is set by the service layer from brandId)
    public Service toEntity(ServiceRequestDTO request) {
        Service service = new Service(
                null,
                request.name().trim(),
                blankToNull(request.description()),
                blankToNull(request.imageUrl())
        );
        service.setFeatured(request.featured());
        return service;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
