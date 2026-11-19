package com.bgsistemas.api.security;

import org.springframework.security.authentication.DisabledException;

// BGSI-238: right password but the admin has not opened the verification email yet
public class AccountNotVerifiedException extends DisabledException {

    public AccountNotVerifiedException() {
        super("Account not verified");
    }
}
