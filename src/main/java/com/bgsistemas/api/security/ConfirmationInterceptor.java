package com.bgsistemas.api.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;

// BGSI-275: before a method marked with @RequiresConfirmation, checks the X-Confirm-Token header.
// The check is done in the server, so calling the API directly without the confirmation also gets 403.
public class ConfirmationInterceptor implements HandlerInterceptor {

    public static final String HEADER = "X-Confirm-Token";

    private final ConfirmationTokenService confirmationTokens;

    public ConfirmationInterceptor(ConfirmationTokenService confirmationTokens) {
        this.confirmationTokens = confirmationTokens;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method) || !method.hasMethodAnnotation(RequiresConfirmation.class)) {
            return true;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication == null ? null : authentication.getName();
        if (!confirmationTokens.isValid(request.getHeader(HEADER), email, Instant.now())) {
            throw new ConfirmationRequiredException(); // GlobalExceptionHandler -> 403
        }
        return true;
    }
}
