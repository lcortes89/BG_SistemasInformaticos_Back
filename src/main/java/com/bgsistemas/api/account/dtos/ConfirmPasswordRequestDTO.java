package com.bgsistemas.api.account.dtos;

import jakarta.validation.constraints.NotBlank;

// BGSI-275: body of POST /api/admin/account/confirm
public record ConfirmPasswordRequestDTO(

        @NotBlank(message = "Password is required")
        String password
) {
}
