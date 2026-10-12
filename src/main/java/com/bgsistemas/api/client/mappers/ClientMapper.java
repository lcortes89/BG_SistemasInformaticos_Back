package com.bgsistemas.api.client.mappers;

import com.bgsistemas.api.client.Client;
import com.bgsistemas.api.client.dtos.AdminClientDTO;
import com.bgsistemas.api.client.dtos.ClientDTO;
import com.bgsistemas.api.client.dtos.ClientRequestDTO;
import org.springframework.stereotype.Component;

@Component
public class ClientMapper {

    // Entity -> public API response
    public ClientDTO toDTO(Client client) {
        return new ClientDTO(
                client.getId(),
                client.getName(),
                client.getBusinessType(),
                client.getTestimonial(),
                client.getLogoUrl(),
                client.getRating()
        );
    }

    // Entity -> admin panel response
    public AdminClientDTO toAdminDTO(Client client) {
        return new AdminClientDTO(
                client.getId(),
                client.getName(),
                client.getBusinessType(),
                client.getTestimonial(),
                client.getLogoUrl(),
                client.getRating(),
                client.isPublished()
        );
    }

    // Admin form -> entity (without id: the database or the update decides it)
    public Client toEntity(ClientRequestDTO request) {
        return new Client(
                null,
                request.name().trim(),
                request.businessType().trim(),
                request.testimonial().trim(),
                request.logoUrl() == null || request.logoUrl().isBlank() ? null : request.logoUrl().trim(),
                request.rating(),
                request.published()
        );
    }
}
