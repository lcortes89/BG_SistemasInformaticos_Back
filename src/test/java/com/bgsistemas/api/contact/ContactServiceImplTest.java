package com.bgsistemas.api.contact;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactServiceImplTest {

    @Mock
    private ContactRepository contactRepository;

    @InjectMocks
    private ContactServiceImpl contactService;

    @Test
    void shouldSaveContactWhenGdprConsentIsGiven() {
        Contact contact = new Contact("Ana López", "ana@mail.com", "600123123",
                "BDP One", "I would like a demo", true);
        when(contactRepository.save(contact)).thenReturn(contact);

        Contact result = contactService.createContact(contact);

        assertThat(result.getName()).isEqualTo("Ana López");
        verify(contactRepository).save(contact);
    }

    @Test
    void shouldRejectContactWithoutGdprConsent() {
        Contact contact = new Contact("Ana López", "ana@mail.com", "600123123",
                "BDP One", "I would like a demo", false);

        assertThatThrownBy(() -> contactService.createContact(contact))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("GDPR");

        verify(contactRepository, never()).save(any());
    }
}