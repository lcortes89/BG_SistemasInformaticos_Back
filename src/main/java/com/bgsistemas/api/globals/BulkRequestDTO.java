package com.bgsistemas.api.globals;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

// BGSI-333: body of the bulk actions { "ids": [1, 2, 3] }. At most one page of a table (50)
public record BulkRequestDTO(

        @NotEmpty(message = "Choose at least one item")
        @Size(max = BulkRequestDTO.MAX_IDS, message = "At most 50 items at once")
        List<@NotNull Long> ids
) {
    public static final int MAX_IDS = 50;
}
