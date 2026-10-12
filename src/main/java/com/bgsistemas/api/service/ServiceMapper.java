package com.bgsistemas.api.service;

import com.bgsistemas.api.brand.Brand;
import org.springframework.stereotype.Component;

@Component
public class ServiceMapper {

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
}