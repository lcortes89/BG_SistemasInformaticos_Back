package com.bgsistemas.api.client.exceptions;

import com.bgsistemas.api.globals.ResourceNotFoundException;

public class ClientNotFoundException extends ResourceNotFoundException {

    public ClientNotFoundException(Long id) {
        super("Client with id " + id + " not found");
    }
}
