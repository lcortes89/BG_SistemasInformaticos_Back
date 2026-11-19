package com.bgsistemas.api.verification;

import com.bgsistemas.api.verification.dtos.VerifyEmailRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class VerificationController {

    private final VerificationService verificationService;

    public VerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    // BGSI-238: POST /api/auth/verify-email { token } -> 204, the account becomes ACTIVE.
    // POST (not GET) so the mail scanners that open links do not spend the single-use token.
    // 400 if the link is invalid, used or expired
    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequestDTO request) {
        verificationService.verifyEmail(request.token());
        return ResponseEntity.noContent().build();
    }
}
