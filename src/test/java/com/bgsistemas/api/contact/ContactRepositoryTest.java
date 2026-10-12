package com.bgsistemas.api.contact;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ContactRepositoryTest {

    @Autowired
    private ContactRepository contactRepository;

    private Contact newContact(String name, ContactStatus status) {
        Contact contact = new Contact(name, "mail@mail.com", null, null, "Message", true);
        contact.setStatus(status);
        return contact;
    }

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

    @Test
    void shouldFindContactsByStatusPageByPage() {
        contactRepository.save(newContact("New 1", ContactStatus.NEW));
        contactRepository.save(newContact("New 2", ContactStatus.NEW));
        contactRepository.save(newContact("New 3", ContactStatus.NEW));
        contactRepository.save(newContact("Contacted", ContactStatus.CONTACTED));

        Page<Contact> firstPage = contactRepository.findByStatus(ContactStatus.NEW, PageRequest.of(0, 2));

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getContent()).allMatch(contact -> contact.getStatus() == ContactStatus.NEW);
    }

    @Test
    void shouldCountContactsByStatus() {
        contactRepository.save(newContact("New 1", ContactStatus.NEW));
        contactRepository.save(newContact("New 2", ContactStatus.NEW));
        contactRepository.save(newContact("Archived", ContactStatus.ARCHIVED));

        assertThat(contactRepository.countByStatus(ContactStatus.NEW)).isEqualTo(2);
        assertThat(contactRepository.countByStatus(ContactStatus.CONTACTED)).isZero();
    }

    @Test
    void shouldReturnAtMostFiveLatestContacts() {
        for (String name : List.of("A", "B", "C", "D", "E", "F")) {
            contactRepository.save(newContact(name, ContactStatus.NEW));
        }

        assertThat(contactRepository.findTop5ByOrderByCreatedAtDesc()).hasSize(5);
    }
}
