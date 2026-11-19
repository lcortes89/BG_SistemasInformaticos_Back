package com.bgsistemas.api.verification.dtos;

import jakarta.validation.constraints.NotBlank;

// BGSI-238: body of POST /api/auth/verify-email (the token of the link)
public record VerifyEmailRequestDTO(

        @NotBlank(message = "Token is required")
        String token
) {
}
