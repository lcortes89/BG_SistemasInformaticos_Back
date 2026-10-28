package com.bgsistemas.api.account.exceptions;

// 400 linked to one field of the form, e.g. currentPassword -> "Current password is not correct"
public class InvalidPasswordChangeException extends RuntimeException {

    private final String field;

    public InvalidPasswordChangeException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
