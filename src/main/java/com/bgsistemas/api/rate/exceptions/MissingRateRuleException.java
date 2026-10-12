package com.bgsistemas.api.rate.exceptions;

// Extends IllegalArgumentException, so the GlobalExceptionHandler already answers 400
public class MissingRateRuleException extends IllegalArgumentException {

    public MissingRateRuleException(String message) {
        super(message);
    }
}
