package com.bgsistemas.api.contact.exceptions;

import com.bgsistemas.api.globals.ResourceNotFoundException;

public class ContactNotFoundException extends ResourceNotFoundException {

    public ContactNotFoundException(Long id) {
        super("Contact with id " + id + " not found");
    }
}
