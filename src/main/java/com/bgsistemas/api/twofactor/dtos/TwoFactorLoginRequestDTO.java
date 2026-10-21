package com.bgsistemas.api.twofactor.dtos;

import jakarta.validation.constraints.NotBlank;

// BGSI-276: body of POST /api/auth/login/2fa (second step of the login)
public record TwoFactorLoginRequestDTO(

        @NotBlank(message = "The login has expired. Please log in again")
        String pendingToken,

        @NotBlank(message = "Code is required")
        String code
) {
}
