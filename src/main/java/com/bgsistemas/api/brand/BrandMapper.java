package com.bgsistemas.api.brand;

import org.springframework.stereotype.Component;

@Component
public class BrandMapper {

    public BrandDTO toDTO(Brand brand) {
        return new BrandDTO(
                brand.getId(),
                brand.getName(),
                brand.getDescription(),
                brand.getCategory(),
                brand.getLogoUrl()
        );
    }
}