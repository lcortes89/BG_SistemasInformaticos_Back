package com.bgsistemas.api.twofactor;

import com.bgsistemas.api.account.exceptions.InvalidPasswordChangeException;

// BGSI-276: 400 on the "code" field of the form (wrong, expired or already used)
public class InvalidCodeException extends InvalidPasswordChangeException {

    public InvalidCodeException() {
        super("code", "Code is not correct");
    }
}
