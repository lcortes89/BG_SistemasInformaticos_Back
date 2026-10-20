package com.bgsistemas.api.account.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// BGSI-272: body of PUT /api/admin/account/password
public record ChangePasswordRequestDTO(

        @NotBlank(message = "Current password is required")
        String currentPassword,

        // 72 = the most that BCrypt can use
        @NotBlank(message = "New password is required")
        @Size(min = 10, max = 72, message = "New password must have between 10 and 72 characters")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
                message = "New password needs an uppercase letter, a lowercase letter and a number")
        String newPassword
) {
}
