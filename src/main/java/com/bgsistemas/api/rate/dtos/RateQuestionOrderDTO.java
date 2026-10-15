package com.bgsistemas.api.rate.dtos;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

// Body of PUT /api/admin/rate-questions/order: every question id, in the new order
public record RateQuestionOrderDTO(
        @NotEmpty(message = "The new order is required")
        List<@NotNull(message = "Question ids cannot be empty") Long> ids
) {
}
