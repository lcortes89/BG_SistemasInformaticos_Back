package com.bgsistemas.api.security;

// BGSI-275: 403 when a sensitive action arrives without a valid X-Confirm-Token
public class ConfirmationRequiredException extends RuntimeException {

    public ConfirmationRequiredException() {
        super("Confirm your password to do this action");
    }
}
