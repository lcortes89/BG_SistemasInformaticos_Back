package com.bgsistemas.api.rate.dtos;

import com.bgsistemas.api.rate.AdjustmentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// Body of POST /api/admin/rate-questions/{id}/options and PUT /api/admin/rate-questions/options/{id}
public record RateOptionRequestDTO(

        @Size(max = 16, message = "Icon must be at most 16 characters")
        String icon,

        @NotBlank(message = "Label is required")
        @Size(max = 100, message = "Label must be at most 100 characters")
        String label,

        @Size(max = 255, message = "Description must be at most 255 characters")
        String description,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.00", message = "Amount cannot be negative")
        @Digits(integer = 8, fraction = 2, message = "Amount can have at most 2 decimals")
        BigDecimal amount,

        @NotNull(message = "Adjustment type is required")
        AdjustmentType adjustmentType
) {
}
