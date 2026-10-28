package com.bgsistemas.api.rate.dtos;

import com.bgsistemas.api.rate.AdjustmentType;

import java.math.BigDecimal;

// One option as the admin panel sees it
public record AdminRateOptionDTO(
        Long id,
        String icon,
        String label,
        String description,
        BigDecimal amount,
        AdjustmentType adjustmentType,
        int position
) {
}
