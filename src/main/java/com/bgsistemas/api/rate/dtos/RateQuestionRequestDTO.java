package com.bgsistemas.api.rate.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Body of POST and PUT /api/admin/rate-questions
public record RateQuestionRequestDTO(

        @NotBlank(message = "Title is required")
        @Size(max = 40, message = "Title must be at most 40 characters")
        String title,

        @NotBlank(message = "Question is required")
        @Size(max = 150, message = "Question must be at most 150 characters")
        String question,

        @Size(max = 255, message = "Hint must be at most 255 characters")
        String hint,

        // Optional: a new question is visible unless the admin says otherwise
        Boolean active
) {
}
