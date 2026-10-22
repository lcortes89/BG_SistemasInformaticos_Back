package com.bgsistemas.api.contact;

import com.bgsistemas.api.contact.exceptions.ContactNotFoundException;
import com.bgsistemas.api.notification.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class ContactServiceImpl implements ContactService {

    private final ContactRepository contactRepository;
    private final NotificationService notificationService;

    public ContactServiceImpl(ContactRepository contactRepository, NotificationService notificationService) {
        this.contactRepository = contactRepository;
        this.notificationService = notificationService;
    }

    @Override
    public Contact createContact(Contact contact) {
        // Business rule: a contact cannot be stored without GDPR consent
        if (!contact.isGdprConsent()) {
            throw new IllegalArgumentException("GDPR consent is required to store a contact");
        }
        Contact saved = contactRepository.save(contact);
        // BGSI-53: email to the business (in the background, it never breaks the form)
        notificationService.notifyNewContact(saved);
        return saved;
    }

    @Override
    public Page<Contact> getContacts(ContactStatus status, Pageable pageable) {
        if (status == null) {
            return contactRepository.findAll(pageable);
        }
        return contactRepository.findByStatus(status, pageable);
    }

    @Override
    public Contact updateContact(Long id, ContactStatus status, String note) {
        if (status == null && note == null) {
            throw new IllegalArgumentException("Send a status or a note to update the contact");
        }

        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new ContactNotFoundException(id));

        if (status != null) {
            contact.setStatus(status);
        }
        if (note != null) {
            // An empty note removes the previous one
            contact.setNote(note.isBlank() ? null : note.trim());
        }
        return contactRepository.save(contact);
    }

    @Override
    public void deleteContact(Long id) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new ContactNotFoundException(id));
        contactRepository.delete(contact);
    }
}
