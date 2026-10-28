package com.bgsistemas.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

// BGSI-274: runs before the HTTP Basic login. A locked email gets 429 without checking the password;
// otherwise the result of the login (200 or 401) is counted.
public class LoginAttemptFilter extends OncePerRequestFilter {

    static final String LOGIN_PATH = "/api/auth/login";

    private final LoginAttemptService loginAttemptService;

    public LoginAttemptFilter(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equals(request.getMethod()) && LOGIN_PATH.equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Optional<String> email = basicEmail(request.getHeader(HttpHeaders.AUTHORIZATION));
        if (email.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        Optional<Duration> locked = loginAttemptService.lockRemaining(email.get(), Instant.now());
        if (locked.isPresent()) {
            long seconds = (locked.get().toMillis() + 999) / 1000; // rounded up
            long minutes = Math.max(1, (seconds + 59) / 60); // rounded up: never "0 minutes"
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(seconds));
            SecurityErrorResponseWriter.write(request, response, HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed attempts. Try again in " + minutes + " minutes",
                    "{\"retryAfterMinutes\":\"" + minutes + "\"}");
            return;
        }

        chain.doFilter(request, response);

        if (response.getStatus() == HttpStatus.UNAUTHORIZED.value()) {
            loginAttemptService.onFailure(email.get(), Instant.now());
        } else if (response.getStatus() == HttpStatus.OK.value()) {
            loginAttemptService.onSuccess(email.get());
        }
    }

    // "Basic base64(email:password)" -> email
    static Optional<String> basicEmail(String header) {
        if (header == null || !header.regionMatches(true, 0, "Basic ", 0, 6)) {
            return Optional.empty();
        }
        try {
            String decoded = new String(Base64.getDecoder().decode(header.substring(6).trim()), StandardCharsets.UTF_8);
            int colon = decoded.indexOf(':');
            return colon > 0 ? Optional.of(decoded.substring(0, colon)) : Optional.empty();
        } catch (IllegalArgumentException e) {
            return Optional.empty(); // not valid Base64: Spring Security answers 401 as usual
        }
    }
}
