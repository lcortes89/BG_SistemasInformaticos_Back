package com.bgsistemas.api.user;

// BGSI-277: PENDING = invited, has not chosen a password yet; BLOCKED = cannot log in
public enum UserStatus {
    ACTIVE, PENDING, BLOCKED
}
