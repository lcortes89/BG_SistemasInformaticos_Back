package com.bgsistemas.api.globals;

import org.springframework.data.domain.Page;

import java.util.List;

// Generic paginated response: works with any type T (ClientDTO, ContactResponseDTO...)
public record PageResponseDTO<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <T> PageResponseDTO<T> from(Page<T> page) {
        return new PageResponseDTO<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }
}
