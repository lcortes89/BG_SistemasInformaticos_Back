package com.bgsistemas.api.contact;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ContactRepositoryTest {

    @Autowired
    private ContactRepository contactRepository;

    @Test
    void shouldSaveContactWithDefaultStatusAndCreationDate() {
        Contact contact = new Contact("Ana López", "ana@mail.com", "600123123",
                "BDP One", "I would like a demo", true);

        Contact saved = contactRepository.save(contact);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(ContactStatus.NEW);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.isGdprConsent()).isTrue();
    }
}