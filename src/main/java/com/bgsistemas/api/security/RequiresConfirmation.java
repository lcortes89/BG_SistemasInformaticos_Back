package com.bgsistemas.api.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// BGSI-275: sensitive action. The request needs the header X-Confirm-Token with a token from
// POST /api/admin/account/confirm (password typed again in the last 5 minutes), or it gets 403.
// Checked by ConfirmationInterceptor.
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresConfirmation {
}
