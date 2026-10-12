package com.bgsistemas.api.contact;

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
}