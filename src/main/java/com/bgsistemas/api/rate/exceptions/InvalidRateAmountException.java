package com.bgsistemas.api.rate.exceptions;

// Extends IllegalArgumentException, so the GlobalExceptionHandler answers 400
public class InvalidRateAmountException extends IllegalArgumentException {

    public InvalidRateAmountException(String message) {
        super(message);
    }
}
