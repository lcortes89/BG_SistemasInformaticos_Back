package com.bgsistemas.api.brand.dtos;

// What the admin panel receives: the brand + "active" + how many services it has
public record AdminBrandDTO(
        Long id,
        String name,
        String description,
        String category,
        String logoUrl,
        boolean active,
        long serviceCount
) {
}
