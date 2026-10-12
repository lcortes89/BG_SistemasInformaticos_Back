package com.bgsistemas.api.contact.dtos;

import com.bgsistemas.api.contact.ContactStatus;
import jakarta.validation.constraints.Size;

// PATCH body: only the fields that change are sent (the others arrive as null)
public record ContactUpdateRequestDTO(

        ContactStatus status,

        @Size(max = 1000, message = "Note must be at most 1000 characters")
        String note
) {
}
