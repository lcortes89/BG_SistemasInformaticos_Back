package com.bgsistemas.api.service.dtos;

// What the admin panel receives: the service + "featured" + its brand
public record AdminServiceDTO(
        Long id,
        String name,
        String description,
        String imageUrl,
        boolean featured,
        Long brandId,
        String brandName
) {
}
