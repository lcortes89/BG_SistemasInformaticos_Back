package com.bgsistemas.api.service.mappers;

import com.bgsistemas.api.brand.Brand;
import com.bgsistemas.api.service.Service;
import com.bgsistemas.api.service.dtos.AdminServiceDTO;
import com.bgsistemas.api.service.dtos.ServiceDTO;
import com.bgsistemas.api.service.dtos.ServiceRequestDTO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ServiceMapper {

    // Entity -> public API response
    public ServiceDTO toDTO(Service service) {
        Brand brand = service.getBrand();

        ServiceDTO dto = new ServiceDTO(
                service.getId(),
                service.getName(),
                service.getDescription(),
                service.getImageUrl(),
                brand != null ? brand.getId() : null,
                brand != null ? brand.getName() : null
        );
        // BGSI-205: full product sheet
        dto.setLongDescription(service.getLongDescription());
        dto.setFeatures(List.copyOf(service.getFeatures()));
        dto.setSectors(List.copyOf(service.getSectors()));
        return dto;
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
                brand != null ? brand.getName() : null,
                service.getLongDescription(),
                List.copyOf(service.getFeatures()),
                List.copyOf(service.getSectors())
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
        service.setLongDescription(blankToNull(request.longDescription()));
        service.setFeatures(cleanList(request.features()));
        service.setSectors(cleanList(request.sectors()));
        return service;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    // Trims every text and drops the empty ones (and line breaks, which the converter uses)
    private List<String> cleanList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.replace("\n", " ").replace("\r", " ").trim())
                .toList();
    }
}
