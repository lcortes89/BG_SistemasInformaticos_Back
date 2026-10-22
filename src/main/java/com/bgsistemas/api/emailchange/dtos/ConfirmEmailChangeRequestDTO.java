package com.bgsistemas.api.emailchange.dtos;

import jakarta.validation.constraints.NotBlank;

// BGSI-299: body of POST /api/auth/confirm-email (the token of the link)
public record ConfirmEmailChangeRequestDTO(

        @NotBlank(message = "Token is required")
        String token
) {
}
