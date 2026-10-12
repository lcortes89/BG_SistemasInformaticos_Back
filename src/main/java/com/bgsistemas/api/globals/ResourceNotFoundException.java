package com.bgsistemas.api.globals;

// Parent class for every "not found" error of the app (always answers 404)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
