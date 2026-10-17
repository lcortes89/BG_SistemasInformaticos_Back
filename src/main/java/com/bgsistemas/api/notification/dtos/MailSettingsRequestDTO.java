package com.bgsistemas.api.notification.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MailSettingsRequestDTO(

        @NotBlank(message = "Email is required")
        @Email(message = "Email format is not valid")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String notifyEmail,

        // Optional: empty = "BG Sistemas Web"
        @Size(max = 60, message = "Sender name must be at most 60 characters")
        String senderName
) {
}
