package com.bgsistemas.api.verification.exceptions;

// 400 (it is an IllegalArgumentException): the link does not exist, was already used or has expired
public class InvalidVerificationTokenException extends IllegalArgumentException {

    public InvalidVerificationTokenException() {
        super("The verification link is not valid or has expired");
    }
}