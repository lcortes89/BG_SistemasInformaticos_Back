package com.bgsistemas.api.service.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

// Body of POST and PUT /api/admin/services
public record ServiceRequestDTO(

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @Size(max = 255, message = "Description must be at most 255 characters")
        String description,

        @Size(max = 255, message = "Image URL must be at most 255 characters")
        String imageUrl,

        boolean featured,

        @NotNull(message = "Brand is required")
        Long brandId,

        // BGSI-205: full product sheet (all optional)
        @Size(max = 2000, message = "Long description must be at most 2000 characters")
        String longDescription,

        @Size(max = 6, message = "At most 6 features")
        List<@Size(max = 100, message = "Each feature must be at most 100 characters") String> features,

        @Size(max = 6, message = "At most 6 sectors")
        List<@Size(max = 40, message = "Each sector must be at most 40 characters") String> sectors
) {
}
