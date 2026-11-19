package com.bgsistemas.api.security;

import com.bgsistemas.api.user.UserStatus;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;

// BGSI-238: runs AFTER the password check, so only who knows the password
// learns that the account is pending. A blocked admin gets the generic 401
public class LoginStatusChecker implements UserDetailsChecker {

    @Override
    public void check(UserDetails details) {
        if (!(details instanceof SecurityUser securityUser) || securityUser.getUser().isActive()) {
            return;
        }
        if (securityUser.getUser().getStatus() == UserStatus.PENDING) {
            throw new AccountNotVerifiedException();
        }
        throw new DisabledException("Account disabled");
    }
}
