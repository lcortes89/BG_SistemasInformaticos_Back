package com.bgsistemas.api.globals;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

// BGSI-333: PATCH /api/admin/{brands|services|clients}/bulk-active { "ids": [1, 2], "active": false }
public record BulkActiveRequestDTO(

        @NotEmpty(message = "Choose at least one item")
        @Size(max = BulkRequestDTO.MAX_IDS, message = "At most 50 items at once")
        List<@NotNull Long> ids,

        @NotNull(message = "Active is required")
        Boolean active
) {
}
