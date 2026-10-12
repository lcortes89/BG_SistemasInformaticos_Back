package com.bgsistemas.api.client.mappers;
import com.bgsistemas.api.client.Client;
import com.bgsistemas.api.client.dtos.ClientDTO;

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