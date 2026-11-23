package com.bgsistemas.api.contact;

import com.bgsistemas.api.globals.BulkResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ContactService {

    Contact createContact(Contact contact);

    // status = null -> all contacts
    // BGSI-302: search = text of the search box (name, email or product), null or empty = all
    Page<Contact> getContacts(ContactStatus status, String search, Pageable pageable);

    // Changes the status and/or the note (null = do not change it)
    Contact updateContact(Long id, ContactStatus status, String note);

    // BGSI-307: spam, tests or a person who asks to be forgotten (GDPR)
    void deleteContact(Long id);

    // BGSI-333: bulk actions on several contacts at once (the missing ids are reported, not an error)
    BulkResult deleteContacts(List<Long> ids);

    BulkResult updateContactsStatus(List<Long> ids, ContactStatus status);
}
