package com.bgsistemas.api.contact;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ContactService {

    Contact createContact(Contact contact);

    // status = null -> all contacts
    // BGSI-302: search = text of the search box (name, email or product), null or empty = all
    Page<Contact> getContacts(ContactStatus status, String search, Pageable pageable);

    // Changes the status and/or the note (null = do not change it)
    Contact updateContact(Long id, ContactStatus status, String note);

    // BGSI-307: spam, tests or a person who asks to be forgotten (GDPR)
    void deleteContact(Long id);
}
