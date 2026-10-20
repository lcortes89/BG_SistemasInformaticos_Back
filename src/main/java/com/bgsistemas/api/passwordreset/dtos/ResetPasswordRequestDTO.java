package com.bgsistemas.api.passwordreset.dtos;

import com.bgsistemas.api.globals.PasswordPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// BGSI-273: body of POST /api/auth/reset-password (the token comes from the link of the email)
public record ResetPasswordRequestDTO(

        @NotBlank(message = "Token is required")
        String token,

        @NotBlank(message = "New password is required")
        @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH, message = PasswordPolicy.SIZE_MESSAGE)
        @Pattern(regexp = PasswordPolicy.PATTERN, message = PasswordPolicy.PATTERN_MESSAGE)
        String newPassword
) {
}
