package com.bgsistemas.api.contact;

import com.bgsistemas.api.contact.exceptions.ContactNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class ContactServiceImpl implements ContactService {

    private final ContactRepository contactRepository;

    public ContactServiceImpl(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    @Override
    public Contact createContact(Contact contact) {
        // Business rule: a contact cannot be stored without GDPR consent
        if (!contact.isGdprConsent()) {
            throw new IllegalArgumentException("GDPR consent is required to store a contact");
        }
        return contactRepository.save(contact);
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
}
