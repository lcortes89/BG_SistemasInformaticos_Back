package com.bgsistemas.api.rate.dtos;

import java.math.BigDecimal;

// Result of the calculation: monthlyPrice = basePrice x factor + additions
public record RateQuoteDTO(
        BigDecimal monthlyPrice,
        BigDecimal basePrice,
        BigDecimal factor,
        BigDecimal additions
) {
}
