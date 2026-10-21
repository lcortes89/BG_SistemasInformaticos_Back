package com.bgsistemas.api.adminuser.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// BGSI-277: body of "invite" and "change email"
public record AdminEmailRequestDTO(

        @NotBlank(message = "Email is required")
        @Email(message = "Email format is not valid")
        String email
) {
}
