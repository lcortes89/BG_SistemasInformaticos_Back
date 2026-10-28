package com.bgsistemas.api.globals;

import java.time.LocalDateTime;
import java.util.Map;

// Same JSON shape for every error of the API
public record ApiErrorDTO(
        int status,
        String error,
        String message,
        String path,
        LocalDateTime timestamp,
        Map<String, String> details   // field -> message (only for validation errors)
) {
}
