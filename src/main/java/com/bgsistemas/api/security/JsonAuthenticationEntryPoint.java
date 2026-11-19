package com.bgsistemas.api.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

// 401: not logged in, or wrong email/password/token
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        // BGSI-238: right password, email not verified yet -> the login page can explain it
        if (authException instanceof AccountNotVerifiedException) {
            SecurityErrorResponseWriter.write(request, response, HttpStatus.UNAUTHORIZED,
                    "Your account is not verified yet. Check your email", "{\"reason\":\"EMAIL_NOT_VERIFIED\"}");
            return;
        }
        SecurityErrorResponseWriter.write(request, response, HttpStatus.UNAUTHORIZED,
                "You must log in to access this resource");
    }
}
