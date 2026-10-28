package com.bgsistemas.api.service.exceptions;

import com.bgsistemas.api.globals.ResourceNotFoundException;

public class ServiceNotFoundException extends ResourceNotFoundException {

    public ServiceNotFoundException(Long id) {
        super("Service with id " + id + " not found");
    }
}
