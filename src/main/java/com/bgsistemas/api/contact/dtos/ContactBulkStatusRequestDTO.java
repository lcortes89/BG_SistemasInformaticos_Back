package com.bgsistemas.api.contact.dtos;

import com.bgsistemas.api.contact.ContactStatus;
import com.bgsistemas.api.globals.BulkRequestDTO;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

// BGSI-333: PATCH /api/admin/contacts/bulk-status { "ids": [1, 2], "status": "CONTACTED" }
public record ContactBulkStatusRequestDTO(

        @NotEmpty(message = "Choose at least one item")
        @Size(max = BulkRequestDTO.MAX_IDS, message = "At most 50 items at once")
        List<@NotNull Long> ids,

        @NotNull(message = "Status is required")
        ContactStatus status
) {
}
