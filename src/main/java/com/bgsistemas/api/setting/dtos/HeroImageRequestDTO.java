package com.bgsistemas.api.setting.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Body of PUT /api/admin/settings/hero
public record HeroImageRequestDTO(

        @NotBlank(message = "Image URL is required")
        @Size(max = 255, message = "Image URL must be at most 255 characters")
        @Pattern(regexp = "^https?://\\S+$", message = "Image URL must start with http:// or https://")
        String imageUrl
) {
}
