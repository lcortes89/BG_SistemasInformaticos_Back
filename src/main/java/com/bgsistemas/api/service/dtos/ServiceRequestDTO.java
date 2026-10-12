package com.bgsistemas.api.service.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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
        Long brandId
) {
}
