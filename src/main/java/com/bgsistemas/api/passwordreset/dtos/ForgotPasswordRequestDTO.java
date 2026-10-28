package com.bgsistemas.api.passwordreset.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// BGSI-273: body of POST /api/auth/forgot-password
public record ForgotPasswordRequestDTO(

        @NotBlank(message = "Email is required")
        @Email(message = "Email format is not valid")
        String email
) {
}
