package com.bgsistemas.api.emailchange.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// BGSI-299: body of POST /api/admin/account/email
public record EmailChangeRequestDTO(

        @NotBlank(message = "Email is required")
        @Email(message = "Email format is not valid")
        String newEmail
) {
}
