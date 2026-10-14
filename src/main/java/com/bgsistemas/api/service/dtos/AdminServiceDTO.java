package com.bgsistemas.api.service.dtos;

import java.util.List;

// What the admin panel receives: the service + "featured" + its brand + the product sheet (BGSI-205)
public record AdminServiceDTO(
        Long id,
        String name,
        String description,
        String imageUrl,
        boolean featured,
        Long brandId,
        String brandName,
        String longDescription,
        List<String> features,
        List<String> sectors,
        String officialUrl
) {
}
