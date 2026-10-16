package com.bgsistemas.api.contact;

import com.bgsistemas.api.contact.exceptions.ContactNotFoundException;
import com.bgsistemas.api.notification.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

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

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ContactServiceImpl contactService;

    private final PageRequest pageRequest = PageRequest.of(0, 10);

    private Contact sampleContact() {
        return new Contact("Ana López", "ana@mail.com", "600123123", "BDP One", "I would like a demo", true);
    }

    @Test
    void shouldSaveContactWhenGdprConsentIsGiven() {
        Contact contact = sampleContact();
        when(contactRepository.save(contact)).thenReturn(contact);

        Contact result = contactService.createContact(contact);

        assertThat(result.getName()).isEqualTo("Ana López");
        verify(contactRepository).save(contact);
        verify(notificationService).notifyNewContact(contact); // BGSI-53
    }

    @Test
    void shouldRejectContactWithoutGdprConsent() {
        Contact contact = new Contact("Ana López", "ana@mail.com", "600123123",
                "BDP One", "I would like a demo", false);

        assertThatThrownBy(() -> contactService.createContact(contact))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("GDPR");

        verify(contactRepository, never()).save(any());
        verify(notificationService, never()).notifyNewContact(any());
    }

    @Test
    void shouldReturnAllContactsWhenNoStatusIsGiven() {
        Contact contact = sampleContact();
        when(contactRepository.findAll(pageRequest)).thenReturn(new PageImpl<>(List.of(contact), pageRequest, 1));

        Page<Contact> result = contactService.getContacts(null, pageRequest);

        assertThat(result.getContent()).hasSize(1);
        verify(contactRepository, never()).findByStatus(any(), any());
    }

    @Test
    void shouldFilterContactsByStatus() {
        when(contactRepository.findByStatus(ContactStatus.NEW, pageRequest))
                .thenReturn(new PageImpl<>(List.of(), pageRequest, 0));

        contactService.getContacts(ContactStatus.NEW, pageRequest);

        verify(contactRepository).findByStatus(ContactStatus.NEW, pageRequest);
        verify(contactRepository, never()).findAll(pageRequest);
    }

    @Test
    void shouldUpdateStatusAndNote() {
        Contact contact = sampleContact();
        when(contactRepository.findById(1L)).thenReturn(Optional.of(contact));
        when(contactRepository.save(contact)).thenReturn(contact);

        Contact result = contactService.updateContact(1L, ContactStatus.CONTACTED, "  Called on Monday  ");

        assertThat(result.getStatus()).isEqualTo(ContactStatus.CONTACTED);
        assertThat(result.getNote()).isEqualTo("Called on Monday");
    }

    @Test
    void shouldKeepStatusAndRemoveNoteWhenNoteIsEmpty() {
        Contact contact = sampleContact();
        contact.setNote("Old note");
        when(contactRepository.findById(1L)).thenReturn(Optional.of(contact));
        when(contactRepository.save(contact)).thenReturn(contact);

        Contact result = contactService.updateContact(1L, null, "");

        assertThat(result.getStatus()).isEqualTo(ContactStatus.NEW);
        assertThat(result.getNote()).isNull();
    }

    @Test
    void shouldThrowWhenContactDoesNotExist() {
        when(contactRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contactService.updateContact(99L, ContactStatus.ARCHIVED, null))
                .isInstanceOf(ContactNotFoundException.class)
                .hasMessageContaining("99");

        verify(contactRepository, never()).save(any());
    }

    @Test
    void shouldRejectUpdateWithoutStatusOrNote() {
        assertThatThrownBy(() -> contactService.updateContact(1L, null, null))
                .isInstanceOf(IllegalArgumentException.class);

        verify(contactRepository, never()).findById(any());
    }
}
