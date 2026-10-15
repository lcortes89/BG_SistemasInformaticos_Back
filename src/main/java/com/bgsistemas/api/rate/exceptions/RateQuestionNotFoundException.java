package com.bgsistemas.api.rate.exceptions;

import com.bgsistemas.api.globals.ResourceNotFoundException;

public class RateQuestionNotFoundException extends ResourceNotFoundException {

    public RateQuestionNotFoundException(Long id) {
        super("Rate question with id " + id + " not found");
    }
}
