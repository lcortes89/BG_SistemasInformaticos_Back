package com.bgsistemas.api.rate.dtos;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

// The answers of the wizard: the id of the option chosen in each question (BGSI-233)
public record RateCalculationRequestDTO(
        @NotEmpty(message = "Choose an answer for every question")
        List<@NotNull(message = "Answer ids cannot be empty") Long> optionIds
) {
}
