package com.bgsistemas.api.contact.mappers;

import com.bgsistemas.api.contact.Contact;
import com.bgsistemas.api.contact.dtos.ContactRequestDTO;
import com.bgsistemas.api.contact.dtos.ContactResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class ContactMapper {

    // Form data -> entity to be saved
    public Contact toEntity(ContactRequestDTO request) {
        return new Contact(
                request.name(),
                request.email(),
                request.phone(),
                request.interestedService(),
                request.message(),
                request.gdprConsent()
        );
    }

    // Saved entity -> API response
    public ContactResponseDTO toResponse(Contact contact) {
        return new ContactResponseDTO(
                contact.getId(),
                contact.getName(),
                contact.getEmail(),
                contact.getPhone(),
                contact.getInterestedService(),
                contact.getMessage(),
                contact.getStatus().name(),
                contact.getCreatedAt(),
                contact.getNote()
        );
    }
}
