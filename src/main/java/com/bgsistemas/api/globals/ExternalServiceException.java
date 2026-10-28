package com.bgsistemas.api.globals;

// Parent class for "an outside service failed" errors, e.g. Gmail (always answers 503)
public class ExternalServiceException extends RuntimeException {

    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
