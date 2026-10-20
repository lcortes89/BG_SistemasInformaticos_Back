package com.bgsistemas.api.globals;

import com.bgsistemas.api.account.exceptions.InvalidPasswordChangeException;
import com.bgsistemas.api.auth.exceptions.EmailAlreadyExistsException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 400: @Valid failed (e.g. empty name, wrong email, GDPR not accepted)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorDTO> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> details.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Some fields are not valid", request, details);
    }

    // 400: the JSON body is malformed or missing
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorDTO> handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "The request body is malformed or missing", request, null);
    }

    // 400: a URL parameter has a value of the wrong type (e.g. ?status=FOO or ?page=abc)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorDTO> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + ex.getName() + "'", request, null);
    }

    // 400: a business rule was broken (e.g. contact without GDPR consent)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorDTO> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    // 400 linked to one field (BGSI-272): wrong current password, new one equal to the current one
    @ExceptionHandler(InvalidPasswordChangeException.class)
    public ResponseEntity<ApiErrorDTO> handleInvalidPasswordChange(InvalidPasswordChangeException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, Map.of(ex.getField(), ex.getMessage()));
    }

    // 404: any of our "not found" exceptions
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorDTO> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    // 409: any of our "conflict" exceptions (e.g. deleting a brand that still has services)
    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ApiErrorDTO> handleConflict(ResourceConflictException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    // 409: email already registered
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiErrorDTO> handleEmailExists(EmailAlreadyExistsException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    // 503: an outside service failed (e.g. Gmail could not send an email)
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ApiErrorDTO> handleExternalService(ExternalServiceException ex, HttpServletRequest request) {
        log.warn("External service error on {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request, null);
    }

    // Anything else: Spring's own errors keep their status (404 route, 405 method...),
    // unexpected ones become 500 without exposing technical details
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDTO> handleUnexpected(Exception ex, HttpServletRequest request) {
        if (ex instanceof ErrorResponse springError) {
            HttpStatus status = HttpStatus.valueOf(springError.getStatusCode().value());
            return build(status, status.getReasonPhrase(), request, null);
        }
        log.error("Unexpected error on {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again later.", request, null);
    }

    private ResponseEntity<ApiErrorDTO> build(HttpStatus status, String message,
                                              HttpServletRequest request, Map<String, String> details) {
        ApiErrorDTO body = new ApiErrorDTO(status.value(), status.getReasonPhrase(), message,
                request.getRequestURI(), LocalDateTime.now(), details);
        return ResponseEntity.status(status).body(body);
    }
}
