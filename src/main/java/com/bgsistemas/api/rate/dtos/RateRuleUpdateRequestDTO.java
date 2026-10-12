package com.bgsistemas.api.rate.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

// Body of PUT /api/admin/rate-rules/{id}: only the amount can change
public record RateRuleUpdateRequestDTO(

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.00", message = "Amount cannot be negative")
        @Digits(integer = 8, fraction = 2, message = "Amount can have at most 2 decimals")
        BigDecimal amount
) {
}
