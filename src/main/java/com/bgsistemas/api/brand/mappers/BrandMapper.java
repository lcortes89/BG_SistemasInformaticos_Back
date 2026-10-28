package com.bgsistemas.api.brand.mappers;

import com.bgsistemas.api.brand.Brand;
import com.bgsistemas.api.brand.dtos.AdminBrandDTO;
import com.bgsistemas.api.brand.dtos.BrandDTO;
import com.bgsistemas.api.brand.dtos.BrandRequestDTO;
import org.springframework.stereotype.Component;

@Component
public class BrandMapper {

    // Entity -> public API response
    public BrandDTO toDTO(Brand brand) {
        return new BrandDTO(
                brand.getId(),
                brand.getName(),
                brand.getDescription(),
                brand.getCategory(),
                brand.getLogoUrl()
        );
    }

    // Entity -> admin panel response
    public AdminBrandDTO toAdminDTO(Brand brand, long serviceCount) {
        return new AdminBrandDTO(
                brand.getId(),
                brand.getName(),
                brand.getDescription(),
                brand.getCategory(),
                brand.getLogoUrl(),
                brand.isActive(),
                serviceCount
        );
    }

    // Admin form -> entity (without id: the database or the update decides it)
    public Brand toEntity(BrandRequestDTO request) {
        return new Brand(
                null,
                request.name().trim(),
                blankToNull(request.description()),
                request.category().trim(),
                blankToNull(request.logoUrl()),
                request.active()
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
