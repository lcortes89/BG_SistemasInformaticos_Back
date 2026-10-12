package com.bgsistemas.api.contact;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ContactService {

    Contact createContact(Contact contact);

    // status = null -> all contacts
    Page<Contact> getContacts(ContactStatus status, Pageable pageable);
}
