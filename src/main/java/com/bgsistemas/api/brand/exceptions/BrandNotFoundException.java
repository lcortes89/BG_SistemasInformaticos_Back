package com.bgsistemas.api.brand.exceptions;

import com.bgsistemas.api.globals.ResourceNotFoundException;

public class BrandNotFoundException extends ResourceNotFoundException {

    public BrandNotFoundException(Long id) {
        super("Brand with id " + id + " not found");
    }
}
