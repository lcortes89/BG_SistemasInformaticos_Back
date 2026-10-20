package com.bgsistemas.api.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.LocalDateTime;

// Writes 401/403/429 errors with the same shape as ApiErrorDTO
// (security errors happen before reaching the controllers, so GlobalExceptionHandler cannot catch them)
final class SecurityErrorResponseWriter {

    private SecurityErrorResponseWriter() {
    }

    static void write(HttpServletRequest request, HttpServletResponse response,
                      HttpStatus status, String message) throws IOException {
        write(request, response, status, message, "null");
    }

    // detailsJson: a JSON object with extra data for the frontend, e.g. {"retryAfterMinutes":"12"} (BGSI-274)
    static void write(HttpServletRequest request, HttpServletResponse response,
                      HttpStatus status, String message, String detailsJson) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String json = String.format(
                "{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\",\"timestamp\":\"%s\",\"details\":%s}",
                status.value(), status.getReasonPhrase(), message, request.getRequestURI(), LocalDateTime.now(), detailsJson);
        response.getWriter().write(json);
    }
}
