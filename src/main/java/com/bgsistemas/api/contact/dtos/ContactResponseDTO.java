package com.bgsistemas.api.contact.dtos;

import java.time.LocalDateTime;

public record ContactResponseDTO(
        Long id,
        String name,
        String email,
        String phone,
        String interestedService,
        String message,
        String status,
        LocalDateTime createdAt,
        String note
) {
}
