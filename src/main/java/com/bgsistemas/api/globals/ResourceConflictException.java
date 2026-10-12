package com.bgsistemas.api.globals;

// Parent class for "this action conflicts with the current data" errors (always answers 409)
public class ResourceConflictException extends RuntimeException {

    public ResourceConflictException(String message) {
        super(message);
    }
}
