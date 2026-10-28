package com.bgsistemas.api.globals;

import jakarta.validation.constraints.NotNull;

// BGSI-309: body of PATCH /api/admin/{clients|brands|services}/{id}/active
public record ActiveRequestDTO(

        @NotNull(message = "Active is required")
        Boolean active
) {
}
