package com.bgsistemas.api.passwordreset.exceptions;

// 400 (it is an IllegalArgumentException): the link does not exist, was already used or has expired
public class InvalidResetTokenException extends IllegalArgumentException {

    public InvalidResetTokenException() {
        super("The link is not valid or has expired");
    }
}
