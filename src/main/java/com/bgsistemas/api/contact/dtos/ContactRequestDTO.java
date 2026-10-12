package com.bgsistemas.api.contact.dtos;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactRequestDTO(

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email format is not valid")
        String email,

        @Size(max = 20, message = "Phone must be at most 20 characters")
        String phone,

        String interestedService,

        @NotBlank(message = "Message is required")
        @Size(max = 2000, message = "Message must be at most 2000 characters")
        String message,

        @AssertTrue(message = "You must accept the privacy policy (GDPR)")
        boolean gdprConsent
) {
}
