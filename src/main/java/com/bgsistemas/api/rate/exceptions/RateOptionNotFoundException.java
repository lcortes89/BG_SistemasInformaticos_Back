package com.bgsistemas.api.rate.exceptions;

import com.bgsistemas.api.globals.ResourceNotFoundException;

public class RateOptionNotFoundException extends ResourceNotFoundException {

    public RateOptionNotFoundException(Long id) {
        super("Rate option with id " + id + " not found");
    }
}
