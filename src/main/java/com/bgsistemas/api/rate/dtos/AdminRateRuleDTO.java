package com.bgsistemas.api.rate.dtos;

import com.bgsistemas.api.rate.AdjustmentType;
import com.bgsistemas.api.rate.RateCategory;

import java.math.BigDecimal;

// One editable rule as the admin panel sees it
public record AdminRateRuleDTO(
        Long id,
        RateCategory category,
        String optionCode,
        String label,
        BigDecimal amount,
        AdjustmentType adjustmentType
) {
}
