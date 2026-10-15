package com.bgsistemas.api.rate.dtos;

import java.util.List;

// One question as the admin panel sees it, with its options and their prices
public record AdminRateQuestionDTO(
        Long id,
        String title,
        String question,
        String hint,
        int position,
        boolean active,
        List<AdminRateOptionDTO> options
) {
}
