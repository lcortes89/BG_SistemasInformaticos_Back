package com.bgsistemas.api.client;

import org.springframework.stereotype.Component;

@Component
public class ClientMapper {

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
}