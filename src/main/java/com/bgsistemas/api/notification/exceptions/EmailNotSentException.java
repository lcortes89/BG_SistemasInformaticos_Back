package com.bgsistemas.api.notification.exceptions;

import com.bgsistemas.api.globals.ExternalServiceException;

// BGSI-260: email not configured or Gmail refused it (the GlobalExceptionHandler answers 503)
public class EmailNotSentException extends ExternalServiceException {

    public EmailNotSentException(String message) {
        super(message);
    }

    public EmailNotSentException(String message, Throwable cause) {
        super(message, cause);
    }
}
