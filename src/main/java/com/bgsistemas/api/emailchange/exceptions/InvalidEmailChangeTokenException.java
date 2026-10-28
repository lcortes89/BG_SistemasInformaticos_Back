package com.bgsistemas.api.emailchange.exceptions;

// 400 (it is an IllegalArgumentException): the link does not exist, was already used, was cancelled or has expired
public class InvalidEmailChangeTokenException extends IllegalArgumentException {

    public InvalidEmailChangeTokenException() {
        super("The link is not valid or has expired");
    }
}
