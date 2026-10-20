package com.bgsistemas.api.passwordreset;

import com.bgsistemas.api.passwordreset.dtos.ForgotPasswordRequestDTO;
import com.bgsistemas.api.passwordreset.dtos.ResetPasswordRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// BGSI-273: public endpoints (the admin cannot log in, that is why they need them)
@RestController
@RequestMapping("/api/auth")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    // POST /api/auth/forgot-password -> always 202, whether the email exists or not
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request) {
        passwordResetService.requestReset(request.email());
        return ResponseEntity.accepted().build();
    }

    // POST /api/auth/reset-password -> 204, or 400 if the link is not valid
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
