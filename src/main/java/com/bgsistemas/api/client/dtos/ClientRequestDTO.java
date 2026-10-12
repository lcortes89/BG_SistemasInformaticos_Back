package com.bgsistemas.api.client.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Body of POST and PUT /api/admin/clients
public record ClientRequestDTO(

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @NotBlank(message = "Business type is required")
        @Size(max = 100, message = "Business type must be at most 100 characters")
        String businessType,

        @NotBlank(message = "Testimonial is required")
        @Size(max = 1000, message = "Testimonial must be at most 1000 characters")
        String testimonial,

        @Size(max = 255, message = "Logo URL must be at most 255 characters")
        String logoUrl,

        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be between 1 and 5")
        @Max(value = 5, message = "Rating must be between 1 and 5")
        Integer rating,

        boolean published
) {
}
