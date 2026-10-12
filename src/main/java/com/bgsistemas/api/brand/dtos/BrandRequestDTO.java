package com.bgsistemas.api.brand.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Body of POST and PUT /api/admin/brands
public record BrandRequestDTO(

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @Size(max = 255, message = "Description must be at most 255 characters")
        String description,

        @NotBlank(message = "Category is required")
        @Size(max = 100, message = "Category must be at most 100 characters")
        String category,

        @Size(max = 255, message = "Logo URL must be at most 255 characters")
        String logoUrl,

        boolean active
) {
}
