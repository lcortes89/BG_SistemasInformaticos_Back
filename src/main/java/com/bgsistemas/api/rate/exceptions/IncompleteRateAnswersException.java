package com.bgsistemas.api.rate.exceptions;

// Extends IllegalArgumentException, so the GlobalExceptionHandler answers 400
public class IncompleteRateAnswersException extends IllegalArgumentException {

    public IncompleteRateAnswersException(String message) {
        super(message);
    }
}
