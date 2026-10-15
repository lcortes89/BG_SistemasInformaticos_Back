package com.bgsistemas.api.rate.dtos;

// One answer card of the public wizard
public record RateStepOptionDTO(
        Long id,
        String icon,
        String label,
        String description
) {
}
