package com.bgsistemas.api.rate.dtos;

import java.util.List;

// One step of the public wizard (GET /api/rates/steps). It has no prices: those stay in the backend
public record RateStepDTO(
        Long id,
        String title,
        String question,
        String hint,
        List<RateStepOptionDTO> options
) {
}
