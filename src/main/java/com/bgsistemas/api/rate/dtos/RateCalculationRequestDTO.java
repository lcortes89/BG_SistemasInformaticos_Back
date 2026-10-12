package com.bgsistemas.api.rate.dtos;

import jakarta.validation.constraints.NotBlank;

// The 5 answers of the wizard (option codes, e.g. "RESTAURANT", "TWO_THREE"...)
public record RateCalculationRequestDTO(

        @NotBlank(message = "Business type is required")
        String businessType,

        @NotBlank(message = "Number of locations is required")
        String locations,

        @NotBlank(message = "Number of points of sale is required")
        String posCount,

        @NotBlank(message = "Space is required")
        String space,

        @NotBlank(message = "Square meters are required")
        String squareMeters
) {
}
