package com.bgsistemas.api.rate.exceptions;

import com.bgsistemas.api.globals.ResourceNotFoundException;

public class RateRuleNotFoundException extends ResourceNotFoundException {

    public RateRuleNotFoundException(Long id) {
        super("Rate rule with id " + id + " not found");
    }
}
