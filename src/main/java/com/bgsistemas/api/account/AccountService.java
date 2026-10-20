package com.bgsistemas.api.account;

// BGSI-272: things the logged-in admin can do with their own account
public interface AccountService {

    // Returns a new JWT: every token issued before the change stops working
    String changePassword(String email, String currentPassword, String newPassword);
}
