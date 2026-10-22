package com.bgsistemas.api.emailchange;

import com.bgsistemas.api.emailchange.dtos.ConfirmEmailChangeRequestDTO;
import com.bgsistemas.api.emailchange.dtos.ConfirmEmailChangeResponseDTO;
import com.bgsistemas.api.emailchange.dtos.EmailChangeRequestDTO;
import com.bgsistemas.api.emailchange.dtos.PendingEmailChangeDTO;
import com.bgsistemas.api.security.RequiresConfirmation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// BGSI-299: change my own email ("Mi cuenta")
@RestController
public class EmailChangeController {

    private final EmailChangeService emailChangeService;

    public EmailChangeController(EmailChangeService emailChangeService) {
        this.emailChangeService = emailChangeService;
    }

    // POST /api/admin/account/email { newEmail } -> 202 { newEmail, expiresAt }
    // Needs the password (BGSI-275). 409 if another admin uses that email, 400 if it is the current one
    @RequiresConfirmation
    @PostMapping("/api/admin/account/email")
    public ResponseEntity<PendingEmailChangeDTO> request(@Valid @RequestBody EmailChangeRequestDTO request,
                                                         Authentication authentication) {
        return ResponseEntity.accepted().body(emailChangeService.request(authentication.getName(), request.newEmail()));
    }

    // GET /api/admin/account/email -> 200 { newEmail, expiresAt } (both null if nothing is pending)
    @GetMapping("/api/admin/account/email")
    public PendingEmailChangeDTO pending(Authentication authentication) {
        return emailChangeService.pending(authentication.getName());
    }

    // DELETE /api/admin/account/email -> 204: the link that was sent stops working
    @DeleteMapping("/api/admin/account/email")
    public ResponseEntity<Void> cancel(Authentication authentication) {
        emailChangeService.cancel(authentication.getName());
        return ResponseEntity.noContent().build();
    }

    // POST /api/auth/confirm-email { token } -> 200 { email } (public: the link may be opened in any browser)
    @PostMapping("/api/auth/confirm-email")
    public ConfirmEmailChangeResponseDTO confirm(@Valid @RequestBody ConfirmEmailChangeRequestDTO request) {
        return new ConfirmEmailChangeResponseDTO(emailChangeService.confirm(request.token()));
    }
}
