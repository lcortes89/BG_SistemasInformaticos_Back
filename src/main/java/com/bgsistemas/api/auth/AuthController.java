package com.bgsistemas.api.auth;

import com.bgsistemas.api.auth.dtos.LoginResponseDTO;
import com.bgsistemas.api.auth.dtos.RegisterRequestDTO;
import com.bgsistemas.api.auth.dtos.RegisterResponseDTO;
import com.bgsistemas.api.security.RequiresConfirmation;
import com.bgsistemas.api.security.SecurityUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // POST /api/auth/login with HTTP Basic (email + password) -> 200 JWT.
    // BGSI-276: with 2FA on -> 202 { twoFactorRequired, pendingToken }. 202 is not counted as a finished login
    // by LoginAttemptFilter, so the failures are not reset until the code is right
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@AuthenticationPrincipal SecurityUser principal) {
        LoginResponseDTO response = authService.login(principal.getUser());
        return ResponseEntity.status(response.needsSecondStep() ? HttpStatus.ACCEPTED : HttpStatus.OK).body(response);
    }

    // POST /api/auth/register -> only an authenticated ADMIN can create another admin. Needs the password (BGSI-275)
    @RequiresConfirmation
    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }
}
