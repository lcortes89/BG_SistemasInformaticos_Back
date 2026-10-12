package com.bgsistemas.api.client.dtos;

// What the admin panel receives: the client + "published"
public record AdminClientDTO(
        Long id,
        String name,
        String businessType,
        String testimonial,
        String logoUrl,
        Integer rating,
        boolean published
) {
}
