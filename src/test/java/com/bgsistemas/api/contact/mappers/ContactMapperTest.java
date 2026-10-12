package com.bgsistemas.api.contact.mappers;

import com.bgsistemas.api.contact.Contact;
import com.bgsistemas.api.contact.ContactStatus;
import com.bgsistemas.api.contact.dtos.ContactRequestDTO;
import com.bgsistemas.api.contact.dtos.ContactResponseDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContactMapperTest {

    private final ContactMapper mapper = new ContactMapper();

    @Test
    void shouldMapRequestToEntity() {
        ContactRequestDTO request = new ContactRequestDTO("Ana López", "ana@mail.com", "600123123",
                "BDP One", "I would like a demo", true);

        Contact contact = mapper.toEntity(request);

        assertThat(contact.getName()).isEqualTo("Ana López");
        assertThat(contact.getEmail()).isEqualTo("ana@mail.com");
        assertThat(contact.getInterestedService()).isEqualTo("BDP One");
        assertThat(contact.isGdprConsent()).isTrue();
        assertThat(contact.getStatus()).isEqualTo(ContactStatus.NEW);
    }

    @Test
    void shouldMapEntityToResponse() {
        Contact contact = new Contact("Ana López", "ana@mail.com", "600123123",
                "BDP One", "I would like a demo", true);
        contact.setId(1L);

        ContactResponseDTO response = mapper.toResponse(contact);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Ana López");
        assertThat(response.status()).isEqualTo("NEW");
    }
}
