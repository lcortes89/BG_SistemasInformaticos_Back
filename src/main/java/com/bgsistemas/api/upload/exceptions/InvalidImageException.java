package com.bgsistemas.api.upload.exceptions;

// Extends IllegalArgumentException, so the GlobalExceptionHandler already answers 400
public class InvalidImageException extends IllegalArgumentException {

    public InvalidImageException(String message) {
        super(message);
    }
}
